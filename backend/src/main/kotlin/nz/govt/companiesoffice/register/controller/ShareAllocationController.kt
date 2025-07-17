package nz.govt.companiesoffice.register.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import nz.govt.companiesoffice.register.dto.CancellationRequest
import nz.govt.companiesoffice.register.dto.PaymentUpdateRequest
import nz.govt.companiesoffice.register.dto.ShareAllocationRequest
import nz.govt.companiesoffice.register.dto.ShareTransferRequest
import nz.govt.companiesoffice.register.entity.ShareAllocation
import nz.govt.companiesoffice.register.service.ShareAllocationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/v1/share-allocations")
@Tag(name = "Share Allocations", description = "Share allocation and transfer management")
class ShareAllocationController(
    private val shareAllocationService: ShareAllocationService,
) {

    @PostMapping("/allocate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Allocate shares to a shareholder",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun allocateShares(
        @RequestBody request: ShareAllocationRequest,
    ): ResponseEntity<ShareAllocation> {
        val allocation = shareAllocationService.allocateShares(
            companyId = request.companyId,
            shareholderId = request.shareholderId,
            shareClass = request.shareClass,
            numberOfShares = request.numberOfShares,
            nominalValue = request.nominalValue,
            amountPaid = request.amountPaid ?: BigDecimal.ZERO,
            allocationDate = request.allocationDate ?: LocalDate.now(),
            certificateNumber = request.certificateNumber,
            restrictions = request.restrictions,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(allocation)
    }

    @PostMapping("/{allocationId}/transfer")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Transfer shares to another shareholder",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun transferShares(
        @PathVariable allocationId: Long,
        @RequestBody request: ShareTransferRequest,
    ): ResponseEntity<ShareAllocation> {
        val newAllocation = shareAllocationService.transferShares(
            allocationId = allocationId,
            toShareholderId = request.toShareholderId,
            transferDate = request.transferDate ?: LocalDate.now(),
            certificateNumber = request.certificateNumber,
        )
        return ResponseEntity.ok(newAllocation)
    }

    @PostMapping("/{allocationId}/payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Update share payment",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun updatePayment(
        @PathVariable allocationId: Long,
        @RequestBody request: PaymentUpdateRequest,
    ): ResponseEntity<ShareAllocation> {
        val updatedAllocation = shareAllocationService.updatePayment(
            allocationId = allocationId,
            additionalPayment = request.additionalPayment,
        )
        return ResponseEntity.ok(updatedAllocation)
    }

    @PostMapping("/{allocationId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Cancel share allocation",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun cancelAllocation(
        @PathVariable allocationId: Long,
        @RequestBody request: CancellationRequest,
    ): ResponseEntity<ShareAllocation> {
        val cancelledAllocation = shareAllocationService.cancelAllocation(
            allocationId = allocationId,
            reason = request.reason,
        )
        return ResponseEntity.ok(cancelledAllocation)
    }

    @GetMapping("/{allocationId}")
    @Operation(summary = "Get share allocation by ID")
    fun getShareAllocation(
        @PathVariable allocationId: Long,
    ): ResponseEntity<ShareAllocation> {
        val allocation = shareAllocationService.getShareAllocationById(allocationId)
        return ResponseEntity.ok(allocation)
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Get all active share allocations for a company")
    fun getCompanyAllocations(
        @PathVariable companyId: Long,
    ): ResponseEntity<List<ShareAllocation>> {
        val allocations = shareAllocationService.getActiveAllocationsByCompany(companyId)
        return ResponseEntity.ok(allocations)
    }

    @GetMapping("/shareholder/{shareholderId}")
    @Operation(summary = "Get all active share allocations for a shareholder")
    fun getShareholderAllocations(
        @PathVariable shareholderId: Long,
    ): ResponseEntity<List<ShareAllocation>> {
        val allocations = shareAllocationService.getActiveAllocationsByShareholder(shareholderId)
        return ResponseEntity.ok(allocations)
    }

    @GetMapping("/company/{companyId}/share-class/{shareClass}")
    @Operation(summary = "Get active allocations by share class")
    fun getAllocationsByShareClass(
        @PathVariable companyId: Long,
        @PathVariable shareClass: String,
    ): ResponseEntity<List<ShareAllocation>> {
        val allocations = shareAllocationService.getActiveAllocationsByShareClass(companyId, shareClass)
        return ResponseEntity.ok(allocations)
    }

    @GetMapping("/company/{companyId}/statistics")
    @Operation(summary = "Get company share statistics")
    fun getCompanyShareStatistics(
        @PathVariable companyId: Long,
    ): ResponseEntity<Map<String, Any>> {
        val statistics = shareAllocationService.getCompanyShareStatistics(companyId)
        return ResponseEntity.ok(statistics)
    }

    @GetMapping("/shareholder/{shareholderId}/portfolio")
    @Operation(summary = "Get shareholder portfolio summary")
    fun getShareholderPortfolio(
        @PathVariable shareholderId: Long,
    ): ResponseEntity<Map<String, Any>> {
        val portfolio = shareAllocationService.getShareholderPortfolio(shareholderId)
        return ResponseEntity.ok(portfolio)
    }
}
