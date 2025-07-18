package nz.govt.companiesoffice.register.dto

import nz.govt.companiesoffice.register.entity.ShareClass
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * DTO for ShareClass entity responses
 */
data class ShareClassDto(
    val id: Long,
    val companyId: Long,
    val className: String,
    val classCode: String,
    val description: String?,
    val isRedeemable: Boolean,
    val isConvertible: Boolean,
    val parValue: BigDecimal?,
    val isNoParValue: Boolean,
    val currency: String,

    // Voting Rights
    val votingRights: String,
    val votesPerShare: Int,
    val votingRestrictions: String?,

    // Dividend Rights
    val dividendRights: String,
    val dividendRate: BigDecimal?,
    val isCumulativeDividend: Boolean,
    val dividendPriority: Int,

    // Capital Distribution Rights
    val capitalDistributionRights: String,
    val liquidationPreferenceMultiple: BigDecimal?,
    val liquidationPriority: Int,

    // Transfer Restrictions
    val isTransferable: Boolean,
    val transferRestrictions: String?,
    val requiresBoardApproval: Boolean,
    val hasPreemptiveRights: Boolean,
    val hasTagAlongRights: Boolean,
    val hasDragAlongRights: Boolean,

    // Administrative
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,

    // Computed fields
    val hasVotingRights: Boolean,
    val hasDividendRights: Boolean,
    val hasLiquidationPreference: Boolean,
    val effectiveVotesPerShare: Int,
    val canTransferFreely: Boolean,
    val displayName: String,
    val rightsSummary: String,
) {
    companion object {
        fun fromEntity(shareClass: ShareClass): ShareClassDto {
            return ShareClassDto(
                id = shareClass.id,
                companyId = shareClass.company.id,
                className = shareClass.className,
                classCode = shareClass.classCode,
                description = shareClass.description,
                isRedeemable = shareClass.isRedeemable,
                isConvertible = shareClass.isConvertible,
                parValue = shareClass.parValue,
                isNoParValue = shareClass.isNoParValue,
                currency = shareClass.currency,
                votingRights = shareClass.votingRights,
                votesPerShare = shareClass.votesPerShare,
                votingRestrictions = shareClass.votingRestrictions,
                dividendRights = shareClass.dividendRights,
                dividendRate = shareClass.dividendRate,
                isCumulativeDividend = shareClass.isCumulativeDividend,
                dividendPriority = shareClass.dividendPriority,
                capitalDistributionRights = shareClass.capitalDistributionRights,
                liquidationPreferenceMultiple = shareClass.liquidationPreferenceMultiple,
                liquidationPriority = shareClass.liquidationPriority,
                isTransferable = shareClass.isTransferable,
                transferRestrictions = shareClass.transferRestrictions,
                requiresBoardApproval = shareClass.requiresBoardApproval,
                hasPreemptiveRights = shareClass.hasPreemptiveRights,
                hasTagAlongRights = shareClass.hasTagAlongRights,
                hasDragAlongRights = shareClass.hasDragAlongRights,
                isActive = shareClass.isActive,
                createdAt = shareClass.createdAt,
                updatedAt = shareClass.updatedAt,
                hasVotingRights = shareClass.hasVotingRights(),
                hasDividendRights = shareClass.hasDividendRights(),
                hasLiquidationPreference = shareClass.hasLiquidationPreference(),
                effectiveVotesPerShare = shareClass.getEffectiveVotesPerShare(),
                canTransferFreely = shareClass.canTransferFreely(),
                displayName = shareClass.getDisplayName(),
                rightsSummary = shareClass.getRightsSummary(),
            )
        }
    }
}

/**
 * DTO for creating a new share class
 */
data class CreateShareClassRequest(
    val className: String,
    val classCode: String,
    val description: String? = null,
    val isRedeemable: Boolean = false,
    val isConvertible: Boolean = false,
    val parValue: BigDecimal? = null,
    val isNoParValue: Boolean = false,
    val currency: String = "NZD",

    // Voting Rights
    val votingRights: String = "NONE",
    val votesPerShare: Int = 0,
    val votingRestrictions: String? = null,

    // Dividend Rights
    val dividendRights: String = "NONE",
    val dividendRate: BigDecimal? = null,
    val isCumulativeDividend: Boolean = false,
    val dividendPriority: Int = 0,

    // Capital Distribution Rights
    val capitalDistributionRights: String = "ORDINARY",
    val liquidationPreferenceMultiple: BigDecimal? = null,
    val liquidationPriority: Int = 0,

    // Transfer Restrictions
    val isTransferable: Boolean = true,
    val transferRestrictions: String? = null,
    val requiresBoardApproval: Boolean = false,
    val hasPreemptiveRights: Boolean = false,
    val hasTagAlongRights: Boolean = false,
    val hasDragAlongRights: Boolean = false,
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (className.isBlank()) {
            errors.add("Class name cannot be blank")
        }
        if (classCode.isBlank()) {
            errors.add("Class code cannot be blank")
        }
        if (classCode.length > 20) {
            errors.add("Class code cannot exceed 20 characters")
        }
        if (className.length > 100) {
            errors.add("Class name cannot exceed 100 characters")
        }
        if (description != null && description.length > 500) {
            errors.add("Description cannot exceed 500 characters")
        }

        // Voting rights validation
        if (votingRights !in listOf("NONE", "ORDINARY", "WEIGHTED", "RESTRICTED")) {
            errors.add("Invalid voting rights value")
        }
        if (votingRights == "NONE" && votesPerShare > 0) {
            errors.add("Cannot have votes per share when voting rights is NONE")
        }
        if (votingRights != "NONE" && votesPerShare <= 0) {
            errors.add("Must have positive votes per share when voting rights is not NONE")
        }

        // Dividend rights validation
        if (dividendRights !in listOf("NONE", "ORDINARY", "PREFERRED", "CUMULATIVE")) {
            errors.add("Invalid dividend rights value")
        }
        if (dividendRights in listOf("PREFERRED", "CUMULATIVE") && dividendRate == null) {
            errors.add("Dividend rate is required for preferred or cumulative dividend rights")
        }
        if (dividendRate != null && (dividendRate < BigDecimal.ZERO || dividendRate > BigDecimal.ONE)) {
            errors.add("Dividend rate must be between 0 and 1 (0% to 100%)")
        }

        // Capital distribution validation
        if (capitalDistributionRights !in listOf("ORDINARY", "PREFERRED", "NONE")) {
            errors.add("Invalid capital distribution rights value")
        }

        // Par value validation
        if (isNoParValue && parValue != null) {
            errors.add("Cannot have par value when shares are designated as no par value")
        }
        if (parValue != null && parValue <= BigDecimal.ZERO) {
            errors.add("Par value must be positive")
        }

        // Liquidation preference validation
        if (liquidationPreferenceMultiple != null && liquidationPreferenceMultiple <= BigDecimal.ZERO) {
            errors.add("Liquidation preference multiple must be positive")
        }

        // Currency validation
        if (currency.length != 3) {
            errors.add("Currency code must be exactly 3 characters")
        }

        return errors
    }
}

/**
 * DTO for updating a share class
 */
data class UpdateShareClassRequest(
    val className: String? = null,
    val description: String? = null,
    val votingRights: String? = null,
    val votesPerShare: Int? = null,
    val dividendRights: String? = null,
    val dividendRate: BigDecimal? = null,
    val isCumulativeDividend: Boolean? = null,
    val dividendPriority: Int? = null,
    val capitalDistributionRights: String? = null,
    val liquidationPreferenceMultiple: BigDecimal? = null,
    val liquidationPriority: Int? = null,
    val transferRestrictions: String? = null,
    val requiresBoardApproval: Boolean? = null,
    val hasPreemptiveRights: Boolean? = null,
    val hasTagAlongRights: Boolean? = null,
    val hasDragAlongRights: Boolean? = null,
)

/**
 * DTO for share class statistics
 */
data class ShareClassStatisticsDto(
    val shareClassId: Long,
    val className: String,
    val classCode: String,
    val allocationCount: Long,
    val totalShares: Long,
    val totalValue: BigDecimal,
)
