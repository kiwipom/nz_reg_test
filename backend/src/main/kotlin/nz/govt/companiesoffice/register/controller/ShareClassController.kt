package nz.govt.companiesoffice.register.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import nz.govt.companiesoffice.register.dto.CreateShareClassRequest
import nz.govt.companiesoffice.register.dto.ShareClassDto
import nz.govt.companiesoffice.register.dto.ShareClassStatisticsDto
import nz.govt.companiesoffice.register.dto.UpdateShareClassRequest
import nz.govt.companiesoffice.register.service.ShareClassService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/companies/{companyId}/share-classes")
@Tag(name = "Share Class Management", description = "APIs for managing company share classes and their rights")
class ShareClassController(
    private val shareClassService: ShareClassService,
) {
    private val logger = LoggerFactory.getLogger(ShareClassController::class.java)

    @Operation(
        summary = "Create a new share class",
        description = "Create a new share class for a company with specific rights and restrictions",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Share class created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Company not found"),
            ApiResponse(responseCode = "409", description = "Share class code or name already exists"),
        ],
    )
    @PostMapping
    @PreAuthorize("@roleBasedAccessControl.canManageCompanyShares(#companyId)")
    fun createShareClass(
        @Parameter(description = "Company ID") @PathVariable companyId: Long,
        @RequestBody request: CreateShareClassRequest,
    ): ResponseEntity<Any> {
        logger.info("Creating share class for company {}: {} ({})", companyId, request.className, request.classCode)

        try {
            // Validate request
            val validationErrors = request.validate()
            if (validationErrors.isNotEmpty()) {
                return ResponseEntity.badRequest().body(mapOf("errors" to validationErrors))
            }

            val shareClass = shareClassService.createShareClass(
                companyId = companyId,
                className = request.className,
                classCode = request.classCode,
                description = request.description,
                votingRights = request.votingRights,
                votesPerShare = request.votesPerShare,
                dividendRights = request.dividendRights,
                dividendRate = request.dividendRate,
                isCumulativeDividend = request.isCumulativeDividend,
                dividendPriority = request.dividendPriority,
                capitalDistributionRights = request.capitalDistributionRights,
                liquidationPreferenceMultiple = request.liquidationPreferenceMultiple,
                liquidationPriority = request.liquidationPriority,
                isRedeemable = request.isRedeemable,
                isConvertible = request.isConvertible,
                parValue = request.parValue,
                isNoParValue = request.isNoParValue,
                currency = request.currency,
                isTransferable = request.isTransferable,
                transferRestrictions = request.transferRestrictions,
                requiresBoardApproval = request.requiresBoardApproval,
                hasPreemptiveRights = request.hasPreemptiveRights,
                hasTagAlongRights = request.hasTagAlongRights,
                hasDragAlongRights = request.hasDragAlongRights,
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(ShareClassDto.fromEntity(shareClass))
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid request for creating share class: {}", e.message)
            return ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            logger.error("Error creating share class for company {}", companyId, e)
            return ResponseEntity.internalServerError().body(mapOf("error" to "Internal server error"))
        }
    }

    @Operation(
        summary = "Get share classes for a company",
        description = "Retrieve all share classes for a company, optionally filtered by active status",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Share classes retrieved successfully"),
            ApiResponse(responseCode = "404", description = "Company not found"),
        ],
    )
    @GetMapping
    fun getShareClasses(
        @Parameter(description = "Company ID") @PathVariable companyId: Long,
        @Parameter(
            description = "Filter by active status",
        ) @RequestParam(required = false, defaultValue = "true") activeOnly:
        Boolean,
    ): ResponseEntity<List<ShareClassDto>> {
        logger.debug("Getting share classes for company {}, activeOnly: {}", companyId, activeOnly)

        val shareClasses = if (activeOnly) {
            shareClassService.getActiveShareClassesByCompany(companyId)
        } else {
            shareClassService.getAllShareClassesByCompany(companyId)
        }

        val shareClassDtos = shareClasses.map { ShareClassDto.fromEntity(it) }
        return ResponseEntity.ok(shareClassDtos)
    }

    @Operation(
        summary = "Get a specific share class",
        description = "Retrieve details of a specific share class by ID",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Share class retrieved successfully"),
            ApiResponse(responseCode = "404", description = "Share class not found"),
        ],
    )
    @GetMapping("/{shareClassId}")
    fun getShareClass(
        @Parameter(description = "Company ID") @PathVariable companyId: Long,
        @Parameter(description = "Share class ID") @PathVariable shareClassId: Long,
    ): ResponseEntity<ShareClassDto> {
        logger.debug("Getting share class {} for company {}", shareClassId, companyId)

        val shareClass = shareClassService.getShareClassById(shareClassId)
            ?: return ResponseEntity.notFound().build()

        // Verify the share class belongs to the specified company
        if (shareClass.company.id != companyId) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(ShareClassDto.fromEntity(shareClass))
    }

    @Operation(
        summary = "Get share class by code",
        description = "Retrieve share class details by company ID and class code",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Share class retrieved successfully"),
            ApiResponse(responseCode = "404", description = "Share class not found"),
        ],
    )
    @GetMapping("/by-code/{classCode}")
    fun getShareClassByCode(
        @Parameter(description = "Company ID") @PathVariable companyId: Long,
        @Parameter(description = "Share class code") @PathVariable classCode: String,
    ): ResponseEntity<ShareClassDto> {
        logger.debug("Getting share class with code {} for company {}", classCode, companyId)

        val shareClass = shareClassService.getShareClassByCode(companyId, classCode)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(ShareClassDto.fromEntity(shareClass))
    }

    @Operation(
        summary = "Update a share class",
        description = "Update properties of an existing share class",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Share class updated successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Share class not found"),
        ],
    )
    @PutMapping("/{shareClassId}")
    @PreAuthorize("@roleBasedAccessControl.canManageCompanyShares(#companyId)")
    fun updateShareClass(
        @Parameter(description = "Company ID") @PathVariable companyId: Long,
        @Parameter(description = "Share class ID") @PathVariable shareClassId: Long,
        @RequestBody request: UpdateShareClassRequest,
    ): ResponseEntity<Any> {
        logger.info("Updating share class {} for company {}", shareClassId, companyId)

        try {
            // Verify the share class exists and belongs to the company
            val existingShareClass = shareClassService.getShareClassById(shareClassId)
                ?: return ResponseEntity.notFound().build()

            if (existingShareClass.company.id != companyId) {
                return ResponseEntity.notFound().build()
            }

            val updatedShareClass = shareClassService.updateShareClass(
                shareClassId = shareClassId,
                className = request.className,
                description = request.description,
                votingRights = request.votingRights,
                votesPerShare = request.votesPerShare,
                dividendRights = request.dividendRights,
                dividendRate = request.dividendRate,
                isCumulativeDividend = request.isCumulativeDividend,
                dividendPriority = request.dividendPriority,
                capitalDistributionRights = request.capitalDistributionRights,
                liquidationPreferenceMultiple = request.liquidationPreferenceMultiple,
                liquidationPriority = request.liquidationPriority,
                transferRestrictions = request.transferRestrictions,
                requiresBoardApproval = request.requiresBoardApproval,
                hasPreemptiveRights = request.hasPreemptiveRights,
                hasTagAlongRights = request.hasTagAlongRights,
                hasDragAlongRights = request.hasDragAlongRights,
            )

            return ResponseEntity.ok(ShareClassDto.fromEntity(updatedShareClass))
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid request for updating share class {}: {}", shareClassId, e.message)
            return ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            logger.error("Error updating share class {} for company {}", shareClassId, companyId, e)
            return ResponseEntity.internalServerError().body(mapOf("error" to "Internal server error"))
        }
    }

    @Operation(
        summary = "Deactivate a share class",
        description = "Soft delete a share class by marking it as inactive",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Share class deactivated successfully"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Share class not found"),
        ],
    )
    @DeleteMapping("/{shareClassId}")
    @PreAuthorize("@roleBasedAccessControl.canManageCompanyShares(#companyId)")
    fun deactivateShareClass(
        @Parameter(description = "Company ID") @PathVariable companyId: Long,
        @Parameter(description = "Share class ID") @PathVariable shareClassId: Long,
    ): ResponseEntity<Any> {
        logger.info("Deactivating share class {} for company {}", shareClassId, companyId)

        try {
            // Verify the share class exists and belongs to the company
            val existingShareClass = shareClassService.getShareClassById(shareClassId)
                ?: return ResponseEntity.notFound().build()

            if (existingShareClass.company.id != companyId) {
                return ResponseEntity.notFound().build()
            }

            val deactivatedShareClass = shareClassService.deactivateShareClass(shareClassId)
            return ResponseEntity.ok(ShareClassDto.fromEntity(deactivatedShareClass))
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid request for deactivating share class {}: {}", shareClassId, e.message)
            return ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            logger.error("Error deactivating share class {} for company {}", shareClassId, companyId, e)
            return ResponseEntity.internalServerError().body(mapOf("error" to "Internal server error"))
        }
    }

    @Operation(
        summary = "Get share class statistics",
        description = "Get statistics for all share classes of a company including allocation counts and values",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            ApiResponse(responseCode = "404", description = "Company not found"),
        ],
    )
    @GetMapping("/statistics")
    fun getShareClassStatistics(
        @Parameter(description = "Company ID") @PathVariable companyId: Long,
    ): ResponseEntity<List<ShareClassStatisticsDto>> {
        logger.debug("Getting share class statistics for company {}", companyId)

        val statistics = shareClassService.getShareClassStatistics(companyId)
        val statisticsDtos = statistics.map { stat ->
            ShareClassStatisticsDto(
                shareClassId = stat["shareClassId"] as Long,
                className = stat["className"] as String,
                classCode = stat["classCode"] as String,
                allocationCount = stat["allocationCount"] as Long,
                totalShares = stat["totalShares"] as Long,
                totalValue = stat["totalValue"] as java.math.BigDecimal,
            )
        }

        return ResponseEntity.ok(statisticsDtos)
    }
}
