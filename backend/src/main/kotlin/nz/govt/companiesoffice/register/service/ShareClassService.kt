package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.audit.AuditAction
import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.ShareClass
import nz.govt.companiesoffice.register.repository.CompanyRepository
import nz.govt.companiesoffice.register.repository.ShareClassRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional
class ShareClassService(
    private val shareClassRepository: ShareClassRepository,
    private val companyRepository: CompanyRepository,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(ShareClassService::class.java)

    /**
     * Create a new share class for a company
     */
    fun createShareClass(
        companyId: Long,
        className: String,
        classCode: String,
        description: String? = null,
        votingRights: String = "NONE",
        votesPerShare: Int = 0,
        dividendRights: String = "NONE",
        dividendRate: BigDecimal? = null,
        isCumulativeDividend: Boolean = false,
        dividendPriority: Int = 0,
        capitalDistributionRights: String = "ORDINARY",
        liquidationPreferenceMultiple: BigDecimal? = null,
        liquidationPriority: Int = 0,
        isRedeemable: Boolean = false,
        isConvertible: Boolean = false,
        parValue: BigDecimal? = null,
        isNoParValue: Boolean = false,
        currency: String = "NZD",
        isTransferable: Boolean = true,
        transferRestrictions: String? = null,
        requiresBoardApproval: Boolean = false,
        hasPreemptiveRights: Boolean = false,
        hasTagAlongRights: Boolean = false,
        hasDragAlongRights: Boolean = false,
    ): ShareClass {
        logger.info("Creating share class for company {}: {} ({})", companyId, className, classCode)

        val company = companyRepository.findById(companyId).orElseThrow {
            IllegalArgumentException("Company not found with ID: $companyId")
        }

        // Validate uniqueness
        if (shareClassRepository.existsByCompany_IdAndClassCode(companyId, classCode)) {
            throw IllegalArgumentException("Share class code '$classCode' already exists for this company")
        }

        if (shareClassRepository.existsByCompany_IdAndClassName(companyId, className)) {
            throw IllegalArgumentException("Share class name '$className' already exists for this company")
        }

        // Validate business rules
        validateShareClassRules(
            votingRights,
            votesPerShare,
            dividendRights,
            dividendRate,
            isCumulativeDividend,
            parValue,
            isNoParValue,
            liquidationPreferenceMultiple,
        )

        val shareClass = ShareClass(
            company = company,
            className = className.trim(),
            classCode = classCode.trim().uppercase(),
            description = description?.trim(),
            votingRights = votingRights,
            votesPerShare = votesPerShare,
            dividendRights = dividendRights,
            dividendRate = dividendRate,
            isCumulativeDividend = isCumulativeDividend,
            dividendPriority = dividendPriority,
            capitalDistributionRights = capitalDistributionRights,
            liquidationPreferenceMultiple = liquidationPreferenceMultiple,
            liquidationPriority = liquidationPriority,
            isRedeemable = isRedeemable,
            isConvertible = isConvertible,
            parValue = parValue,
            isNoParValue = isNoParValue,
            currency = currency,
            isTransferable = isTransferable,
            transferRestrictions = transferRestrictions?.trim(),
            requiresBoardApproval = requiresBoardApproval,
            hasPreemptiveRights = hasPreemptiveRights,
            hasTagAlongRights = hasTagAlongRights,
            hasDragAlongRights = hasDragAlongRights,
        )

        val savedShareClass = shareClassRepository.save(shareClass)

        auditService.logEvent(
            action = AuditAction.CREATE,
            resourceType = "ShareClass",
            resourceId = savedShareClass.id.toString(),
            details = mapOf(
                "className" to className,
                "classCode" to classCode,
                "companyId" to company.id,
                "companyName" to company.companyName,
            ),
        )

        logger.info("Successfully created share class {} for company {}", savedShareClass.id, companyId)
        return savedShareClass
    }

    /**
     * Update an existing share class
     */
    fun updateShareClass(
        shareClassId: Long,
        className: String? = null,
        description: String? = null,
        votingRights: String? = null,
        votesPerShare: Int? = null,
        dividendRights: String? = null,
        dividendRate: BigDecimal? = null,
        isCumulativeDividend: Boolean? = null,
        dividendPriority: Int? = null,
        capitalDistributionRights: String? = null,
        liquidationPreferenceMultiple: BigDecimal? = null,
        liquidationPriority: Int? = null,
        transferRestrictions: String? = null,
        requiresBoardApproval: Boolean? = null,
        hasPreemptiveRights: Boolean? = null,
        hasTagAlongRights: Boolean? = null,
        hasDragAlongRights: Boolean? = null,
    ): ShareClass {
        logger.info("Updating share class {}", shareClassId)

        val shareClass = shareClassRepository.findById(shareClassId).orElseThrow {
            IllegalArgumentException("Share class not found with ID: $shareClassId")
        }

        // Check if class name uniqueness would be violated
        if (className != null && className != shareClass.className) {
            if (shareClassRepository.existsByCompany_IdAndClassName(shareClass.company.id, className)) {
                throw IllegalArgumentException("Share class name '$className' already exists for this company")
            }
            shareClass.className = className.trim()
        }

        // Update fields if provided
        description?.let { shareClass.description = it.trim() }
        votingRights?.let { shareClass.votingRights = it }
        votesPerShare?.let { shareClass.votesPerShare = it }
        dividendRights?.let { shareClass.dividendRights = it }
        dividendRate?.let { shareClass.dividendRate = it }
        isCumulativeDividend?.let { shareClass.isCumulativeDividend = it }
        dividendPriority?.let { shareClass.dividendPriority = it }
        capitalDistributionRights?.let { shareClass.capitalDistributionRights = it }
        liquidationPreferenceMultiple?.let { shareClass.liquidationPreferenceMultiple = it }
        liquidationPriority?.let { shareClass.liquidationPriority = it }
        transferRestrictions?.let { shareClass.transferRestrictions = it.trim() }
        requiresBoardApproval?.let { shareClass.requiresBoardApproval = it }
        hasPreemptiveRights?.let { shareClass.hasPreemptiveRights = it }
        hasTagAlongRights?.let { shareClass.hasTagAlongRights = it }
        hasDragAlongRights?.let { shareClass.hasDragAlongRights = it }

        shareClass.updatedAt = LocalDateTime.now()

        // Validate business rules
        validateShareClassRules(
            shareClass.votingRights,
            shareClass.votesPerShare,
            shareClass.dividendRights,
            shareClass.dividendRate,
            shareClass.isCumulativeDividend,
            shareClass.parValue,
            shareClass.isNoParValue,
            shareClass.liquidationPreferenceMultiple,
        )

        val savedShareClass = shareClassRepository.save(shareClass)

        auditService.logEvent(
            action = AuditAction.UPDATE,
            resourceType = "ShareClass",
            resourceId = savedShareClass.id.toString(),
            details = mapOf(
                "className" to savedShareClass.className,
                "classCode" to savedShareClass.classCode,
                "companyId" to savedShareClass.company.id,
            ),
        )

        logger.info("Successfully updated share class {}", shareClassId)
        return savedShareClass
    }

    /**
     * Get share class by ID
     */
    @Transactional(readOnly = true)
    fun getShareClassById(shareClassId: Long): ShareClass? {
        return shareClassRepository.findById(shareClassId).orElse(null)
    }

    /**
     * Get all active share classes for a company
     */
    @Transactional(readOnly = true)
    fun getActiveShareClassesByCompany(companyId: Long): List<ShareClass> {
        return shareClassRepository.findByCompany_IdAndIsActiveTrue(companyId)
    }

    /**
     * Get all share classes for a company (including inactive)
     */
    @Transactional(readOnly = true)
    fun getAllShareClassesByCompany(companyId: Long): List<ShareClass> {
        return shareClassRepository.findByCompany_IdOrderByCreatedAtAsc(companyId)
    }

    /**
     * Get share class by company and class code
     */
    @Transactional(readOnly = true)
    fun getShareClassByCode(companyId: Long, classCode: String): ShareClass? {
        return shareClassRepository.findByCompany_IdAndClassCode(companyId, classCode)
    }

    /**
     * Deactivate a share class (soft delete)
     */
    fun deactivateShareClass(shareClassId: Long): ShareClass {
        logger.info("Deactivating share class {}", shareClassId)

        val shareClass = shareClassRepository.findById(shareClassId).orElseThrow {
            IllegalArgumentException("Share class not found with ID: $shareClassId")
        }

        // Check if there are active share allocations for this class
        // This would need to be implemented in ShareAllocationService
        // For now, we'll allow deactivation but log a warning

        shareClass.isActive = false
        shareClass.updatedAt = LocalDateTime.now()

        val savedShareClass = shareClassRepository.save(shareClass)

        auditService.logEvent(
            action = AuditAction.DELETE,
            resourceType = "ShareClass",
            resourceId = savedShareClass.id.toString(),
            details = mapOf(
                "className" to savedShareClass.className,
                "classCode" to savedShareClass.classCode,
                "companyId" to savedShareClass.company.id,
                "action" to "deactivation",
            ),
        )

        logger.info("Successfully deactivated share class {}", shareClassId)
        return savedShareClass
    }

    /**
     * Get share class statistics for a company
     */
    @Transactional(readOnly = true)
    fun getShareClassStatistics(companyId: Long): List<Map<String, Any>> {
        val stats = shareClassRepository.getShareClassStatistics(companyId)
        return stats.map { row ->
            mapOf(
                "shareClassId" to row[0],
                "className" to row[1],
                "classCode" to row[2],
                "allocationCount" to row[3],
                "totalShares" to row[4],
                "totalValue" to row[5],
            )
        }
    }

    /**
     * Create default ordinary share class for a company
     */
    fun createDefaultOrdinaryShares(company: Company): ShareClass {
        logger.info("Creating default ordinary share class for company {}", company.id)

        return createShareClass(
            companyId = company.id,
            className = "Ordinary Shares",
            classCode = "ORDINARY",
            description = "Standard ordinary shares with full voting rights and participation in dividends",
            votingRights = "ORDINARY",
            votesPerShare = 1,
            dividendRights = "ORDINARY",
            capitalDistributionRights = "ORDINARY",
            isTransferable = true,
            hasPreemptiveRights = true,
        )
    }

    /**
     * Validate share class business rules
     */
    private fun validateShareClassRules(
        votingRights: String,
        votesPerShare: Int,
        dividendRights: String,
        dividendRate: BigDecimal?,
        isCumulativeDividend: Boolean,
        parValue: BigDecimal?,
        isNoParValue: Boolean,
        liquidationPreferenceMultiple: BigDecimal?,
    ) {
        // Voting rights consistency
        if (votingRights == "NONE" && votesPerShare > 0) {
            throw IllegalArgumentException("Cannot have votes per share when voting rights is NONE")
        }
        if (votingRights != "NONE" && votesPerShare <= 0) {
            throw IllegalArgumentException("Must have positive votes per share when voting rights is not NONE")
        }

        // Dividend rate consistency
        if (dividendRights in listOf("PREFERRED", "CUMULATIVE") && dividendRate == null) {
            throw IllegalArgumentException("Dividend rate is required for preferred or cumulative dividend rights")
        }
        if (dividendRate != null && (dividendRate < BigDecimal.ZERO || dividendRate > BigDecimal.ONE)) {
            throw IllegalArgumentException("Dividend rate must be between 0 and 1 (0% to 100%)")
        }

        // Par value consistency
        if (isNoParValue && parValue != null) {
            throw IllegalArgumentException("Cannot have par value when shares are designated as no par value")
        }
        if (parValue != null && parValue <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Par value must be positive")
        }

        // Liquidation preference consistency
        if (liquidationPreferenceMultiple != null && liquidationPreferenceMultiple <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Liquidation preference multiple must be positive")
        }
    }
}
