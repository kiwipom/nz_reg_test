package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.entity.Address
import nz.govt.companiesoffice.register.entity.AddressType
import nz.govt.companiesoffice.register.entity.Company
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Represents an address change workflow with validation and approval steps
 */
data class AddressChangeWorkflow(
    val companyId: Long,
    val addressType: AddressType,
    val currentAddress: Address?,
    val proposedAddress: Address,
    val effectiveDate: LocalDate,
    val status: AddressChangeStatus,
    val validationResult: AddressValidationService.ValidationResult,
    val requestedBy: String? = null,
    val requestedAt: LocalDateTime,
    val approvedAt: LocalDateTime? = null,
    val rejectedAt: LocalDateTime? = null,
    val rejectionReason: String? = null,
    val executedAddress: Address? = null,
) {
    fun isApproved(): Boolean = status == AddressChangeStatus.APPROVED
    fun isRejected(): Boolean = status == AddressChangeStatus.REJECTED
    fun isPending(): Boolean = status == AddressChangeStatus.PENDING_APPROVAL
    fun hasValidationErrors(): Boolean = !validationResult.isValid
    fun hasValidationWarnings(): Boolean = validationResult.hasWarnings()
}

/**
 * Status of an address change workflow
 */
enum class AddressChangeStatus {
    VALIDATION_FAILED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
}

/**
 * Represents a single address update in a bulk operation
 */
data class AddressUpdate(
    val addressType: AddressType,
    val newAddress: Address,
    val requestedBy: String? = null,
)

/**
 * Result of a bulk address update operation
 */
data class BulkAddressUpdateResult(
    val company: Company,
    val workflows: List<AddressChangeWorkflow>,
    val errors: List<String>,
    val status: BulkUpdateStatus,
) {
    fun isSuccessful(): Boolean = status == BulkUpdateStatus.COMPLETED
    fun requiresApproval(): Boolean = status == BulkUpdateStatus.PENDING_APPROVAL
    fun hasFailed(): Boolean = status == BulkUpdateStatus.VALIDATION_FAILED
    fun getSuccessCount(): Int = workflows.count { it.isApproved() }
    fun getPendingCount(): Int = workflows.count { it.isPending() }
    fun getFailedCount(): Int = workflows.count { it.hasValidationErrors() }
}

/**
 * Status of a bulk address update operation
 */
enum class BulkUpdateStatus {
    VALIDATION_FAILED,
    PENDING_APPROVAL,
    COMPLETED,
}

/**
 * Address change request for API endpoints
 */
data class AddressChangeRequest(
    val addressType: AddressType,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val region: String? = null,
    val postcode: String? = null,
    val country: String = "NZ",
    val email: String? = null,
    val phone: String? = null,
    val effectiveDate: LocalDate = LocalDate.now(),
    val requestedBy: String? = null,
) {
    fun toAddress(company: Company): Address {
        return Address(
            company = company,
            addressType = addressType,
            addressLine1 = addressLine1,
            addressLine2 = addressLine2,
            city = city,
            region = region,
            postcode = postcode,
            country = country,
            email = email,
            phone = phone,
            effectiveFrom = effectiveDate,
        )
    }
}

/**
 * Bulk address change request for API endpoints
 */
data class BulkAddressChangeRequest(
    val addresses: List<AddressChangeRequest>,
    val effectiveDate: LocalDate = LocalDate.now(),
) {
    fun toAddressUpdates(company: Company): List<AddressUpdate> {
        return addresses.map { request ->
            AddressUpdate(
                addressType = request.addressType,
                newAddress = request.toAddress(company),
                requestedBy = request.requestedBy,
            )
        }
    }
}

/**
 * Response for address change operations
 */
data class AddressChangeResponse(
    val success: Boolean,
    val workflow: AddressChangeWorkflow? = null,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
) {
    companion object {
        fun success(workflow: AddressChangeWorkflow): AddressChangeResponse {
            return AddressChangeResponse(
                success = true,
                workflow = workflow,
                warnings = workflow.validationResult.warnings,
                suggestions = workflow.validationResult.suggestions,
            )
        }

        fun failure(errors: List<String>): AddressChangeResponse {
            return AddressChangeResponse(
                success = false,
                errors = errors,
            )
        }

        fun validation(workflow: AddressChangeWorkflow): AddressChangeResponse {
            return AddressChangeResponse(
                success = workflow.validationResult.isValid,
                workflow = if (workflow.validationResult.isValid) workflow else null,
                errors = workflow.validationResult.errors,
                warnings = workflow.validationResult.warnings,
                suggestions = workflow.validationResult.suggestions,
            )
        }
    }
}

/**
 * Response for bulk address change operations
 */
data class BulkAddressChangeResponse(
    val success: Boolean,
    val result: BulkAddressUpdateResult? = null,
    val summary: BulkUpdateSummary,
) {
    companion object {
        fun fromResult(result: BulkAddressUpdateResult): BulkAddressChangeResponse {
            return BulkAddressChangeResponse(
                success = result.isSuccessful(),
                result = result,
                summary = BulkUpdateSummary(
                    totalRequests = result.workflows.size,
                    successfulUpdates = result.getSuccessCount(),
                    pendingApproval = result.getPendingCount(),
                    validationFailures = result.getFailedCount(),
                    errors = result.errors,
                ),
            )
        }
    }
}

/**
 * Summary of bulk update results
 */
data class BulkUpdateSummary(
    val totalRequests: Int,
    val successfulUpdates: Int,
    val pendingApproval: Int,
    val validationFailures: Int,
    val errors: List<String>,
) {
    fun getSuccessRate(): Double {
        return if (totalRequests > 0) {
            successfulUpdates.toDouble() / totalRequests.toDouble()
        } else {
            0.0
        }
    }

    fun hasErrors(): Boolean = errors.isNotEmpty() || validationFailures > 0
}
