package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.ShareAllocation
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.CompanyRepository
import nz.govt.companiesoffice.register.repository.ShareAllocationRepository
import nz.govt.companiesoffice.register.repository.ShareholderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class ShareAllocationService(
    private val shareAllocationRepository: ShareAllocationRepository,
    private val shareholderRepository: ShareholderRepository,
    private val companyRepository: CompanyRepository,
    private val auditService: AuditService,
    private val shareholderNotificationService: ShareholderNotificationService,
) {
    private val logger = LoggerFactory.getLogger(ShareAllocationService::class.java)

    fun allocateShares(
        companyId: Long,
        shareholderId: Long,
        shareClass: String,
        numberOfShares: Long,
        nominalValue: BigDecimal,
        amountPaid: BigDecimal = BigDecimal.ZERO,
        allocationDate: LocalDate = LocalDate.now(),
        certificateNumber: String? = null,
        restrictions: String? = null,
    ): ShareAllocation {
        logger.info("Allocating $numberOfShares shares of class '$shareClass' to shareholder $shareholderId")

        // Validate inputs
        if (numberOfShares <= 0) {
            throw ValidationException("numberOfShares", "Number of shares must be positive")
        }
        if (nominalValue <= BigDecimal.ZERO) {
            throw ValidationException("nominalValue", "Nominal value must be positive")
        }
        if (amountPaid < BigDecimal.ZERO) {
            throw ValidationException("amountPaid", "Amount paid cannot be negative")
        }

        val company = companyRepository.findById(companyId)
            .orElseThrow { ResourceNotFoundException("company", "Company not found with id: $companyId") }

        val shareholder = shareholderRepository.findById(shareholderId)
            .orElseThrow { ResourceNotFoundException("shareholder", "Shareholder not found with id: $shareholderId") }

        // Check if certificate number is unique (if provided)
        if (certificateNumber != null &&
            shareAllocationRepository.findByCertificateNumber(certificateNumber).isNotEmpty()
        ) {
            throw ValidationException("certificateNumber", "Certificate number already exists")
        }

        val totalValue = nominalValue.multiply(BigDecimal(numberOfShares))
        val isFullyPaid = amountPaid >= totalValue

        val allocation = ShareAllocation(
            company = company,
            shareholder = shareholder,
            shareClass = shareClass,
            numberOfShares = numberOfShares,
            nominalValue = nominalValue,
            amountPaid = amountPaid,
            allocationDate = allocationDate,
            certificateNumber = certificateNumber,
            isFullyPaid = isFullyPaid,
            restrictions = restrictions,
            status = "ACTIVE",
        )

        val savedAllocation = shareAllocationRepository.save(allocation)

        auditService.logShareAllocation(
            companyId,
            shareholderId,
            shareClass,
            numberOfShares,
            nominalValue,
        )

        // Send allocation notifications
        shareholderNotificationService.notifyShareAllocation(savedAllocation)

        logger.info("Share allocation created successfully with id: ${savedAllocation.id}")
        return savedAllocation
    }

    fun transferShares(
        allocationId: Long,
        toShareholderId: Long,
        transferDate: LocalDate = LocalDate.now(),
        certificateNumber: String? = null,
    ): ShareAllocation {
        logger.info("Transferring shares from allocation $allocationId to shareholder $toShareholderId")

        val allocation = shareAllocationRepository.findById(allocationId)
            .orElseThrow {
                ResourceNotFoundException(
                    "allocation",
                    "Share allocation not found with id: $allocationId",
                )
            }

        if (allocation.status != "ACTIVE") {
            throw ValidationException("status", "Cannot transfer shares from non-active allocation")
        }

        val toShareholder = shareholderRepository.findById(toShareholderId)
            .orElseThrow {
                ResourceNotFoundException(
                    "shareholder",
                    "Target shareholder not found with id: $toShareholderId",
                )
            }

        // Create new allocation for the recipient
        val newAllocation = ShareAllocation(
            company = allocation.company,
            shareholder = toShareholder,
            shareClass = allocation.shareClass,
            numberOfShares = allocation.numberOfShares,
            nominalValue = allocation.nominalValue,
            amountPaid = allocation.amountPaid,
            allocationDate = transferDate,
            certificateNumber = certificateNumber,
            isFullyPaid = allocation.isFullyPaid,
            restrictions = allocation.restrictions,
            status = "ACTIVE",
        )

        // Mark original allocation as transferred
        allocation.status = "TRANSFERRED"
        allocation.transferDate = transferDate
        allocation.transferToShareholderId = toShareholderId
        allocation.updatedAt = LocalDateTime.now()

        shareAllocationRepository.save(allocation)
        val savedNewAllocation = shareAllocationRepository.save(newAllocation)

        auditService.logShareTransfer(
            allocation.company.id!!,
            allocation.shareholder.id,
            toShareholderId,
            allocation.shareClass,
            allocation.numberOfShares,
        )

        // Send transfer notifications
        shareholderNotificationService.notifyShareTransfer(allocation, savedNewAllocation, transferDate)

        logger.info("Share transfer completed successfully")
        return savedNewAllocation
    }

    fun updatePayment(
        allocationId: Long,
        additionalPayment: BigDecimal,
    ): ShareAllocation {
        logger.info("Updating payment for allocation $allocationId with additional amount: $additionalPayment")

        if (additionalPayment <= BigDecimal.ZERO) {
            throw ValidationException("additionalPayment", "Additional payment must be positive")
        }

        val allocation = shareAllocationRepository.findById(allocationId)
            .orElseThrow {
                ResourceNotFoundException(
                    "allocation",
                    "Share allocation not found with id: $allocationId",
                )
            }

        if (allocation.status != "ACTIVE") {
            throw ValidationException("status", "Cannot update payment for non-active allocation")
        }

        val newAmountPaid = allocation.amountPaid.add(additionalPayment)
        val totalValue = allocation.getTotalValue()

        if (newAmountPaid > totalValue) {
            throw ValidationException("additionalPayment", "Total payment cannot exceed share value")
        }

        allocation.amountPaid = newAmountPaid
        allocation.isFullyPaid = newAmountPaid >= totalValue
        allocation.updatedAt = LocalDateTime.now()

        val savedAllocation = shareAllocationRepository.save(allocation)

        auditService.logSharePaymentUpdate(
            allocation.company.id!!,
            allocation.shareholder.id,
            allocationId,
            additionalPayment,
        )

        // Send payment update notifications
        shareholderNotificationService.notifyPaymentUpdate(savedAllocation, additionalPayment)

        logger.info("Payment updated successfully for allocation $allocationId")
        return savedAllocation
    }

    fun cancelAllocation(allocationId: Long, reason: String): ShareAllocation {
        logger.info("Cancelling allocation $allocationId with reason: $reason")

        val allocation = shareAllocationRepository.findById(allocationId)
            .orElseThrow {
                ResourceNotFoundException(
                    "allocation",
                    "Share allocation not found with id: $allocationId",
                )
            }

        if (allocation.status != "ACTIVE") {
            throw ValidationException("status", "Cannot cancel non-active allocation")
        }

        allocation.status = "CANCELLED"
        allocation.restrictions = if (allocation.restrictions != null) {
            "${allocation.restrictions}; CANCELLED: $reason"
        } else {
            "CANCELLED: $reason"
        }
        allocation.updatedAt = LocalDateTime.now()

        val savedAllocation = shareAllocationRepository.save(allocation)

        auditService.logShareCancellation(
            allocation.company.id!!,
            allocation.shareholder.id,
            allocationId,
            reason,
        )

        // Send cancellation notifications
        shareholderNotificationService.notifyAllocationCancellation(savedAllocation, reason)

        logger.info("Allocation $allocationId cancelled successfully")
        return savedAllocation
    }

    @Transactional(readOnly = true)
    fun getShareAllocationById(allocationId: Long): ShareAllocation {
        return shareAllocationRepository.findById(allocationId)
            .orElseThrow {
                ResourceNotFoundException(
                    "allocation",
                    "Share allocation not found with id: $allocationId",
                )
            }
    }

    @Transactional(readOnly = true)
    fun getActiveAllocationsByCompany(companyId: Long): List<ShareAllocation> {
        return shareAllocationRepository.findByCompanyIdAndStatus(companyId, "ACTIVE")
    }

    @Transactional(readOnly = true)
    fun getActiveAllocationsByShareholder(shareholderId: Long): List<ShareAllocation> {
        return shareAllocationRepository.findByShareholderIdAndStatus(shareholderId, "ACTIVE")
    }

    @Transactional(readOnly = true)
    fun getActiveAllocationsByShareClass(companyId: Long, shareClass: String): List<ShareAllocation> {
        return shareAllocationRepository.findByCompanyIdAndShareClassAndStatus(companyId, shareClass, "ACTIVE")
    }

    @Transactional(readOnly = true)
    fun getCompanyShareStatistics(companyId: Long): Map<String, Any> {
        val totalShares = shareAllocationRepository.getTotalActiveShares(companyId)
        val totalValue = shareAllocationRepository.getTotalActiveShareValue(companyId)
        val totalPaid = shareAllocationRepository.getTotalPaidAmount(companyId)
        val shareClassStats = shareAllocationRepository.getShareClassStatistics(companyId)
        val shareholderStats = shareAllocationRepository.getShareholderOwnershipSummary(companyId)
        val unpaidAllocations = shareAllocationRepository.findByCompanyIdAndIsFullyPaidFalse(companyId)

        return mapOf(
            "totalActiveShares" to totalShares,
            "totalShareValue" to totalValue,
            "totalAmountPaid" to totalPaid,
            "unpaidAmount" to totalValue.subtract(totalPaid),
            "shareClassBreakdown" to shareClassStats,
            "shareholderBreakdown" to shareholderStats,
            "unpaidAllocationsCount" to unpaidAllocations.size,
            "shareClasses" to shareAllocationRepository.getDistinctShareClasses(companyId),
        )
    }

    @Transactional(readOnly = true)
    fun getShareholderPortfolio(shareholderId: Long): Map<String, Any> {
        val activeAllocations = shareAllocationRepository.findByShareholderIdAndStatus(shareholderId, "ACTIVE")
        val totalShares = activeAllocations.sumOf { it.numberOfShares }
        val totalValue = activeAllocations.sumOf { it.getTotalValue() }
        val totalPaid = activeAllocations.sumOf { it.amountPaid }
        val shareClassBreakdown = activeAllocations.groupBy { it.shareClass }
            .mapValues { (_, allocations) ->
                mapOf(
                    "totalShares" to allocations.sumOf { it.numberOfShares },
                    "totalValue" to allocations.sumOf { it.getTotalValue() },
                    "totalPaid" to allocations.sumOf { it.amountPaid },
                )
            }

        return mapOf(
            "totalShares" to totalShares,
            "totalValue" to totalValue,
            "totalPaid" to totalPaid,
            "unpaidAmount" to totalValue.subtract(totalPaid),
            "shareClassBreakdown" to shareClassBreakdown,
            "activeAllocations" to activeAllocations.size,
        )
    }
}
