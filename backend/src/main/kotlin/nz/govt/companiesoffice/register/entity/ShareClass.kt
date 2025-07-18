package nz.govt.companiesoffice.register.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "share_classes")
class ShareClass(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,

    @Column(name = "class_name", nullable = false, length = 100)
    var className: String,

    @Column(name = "class_code", nullable = false, length = 20)
    var classCode: String,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "is_redeemable", nullable = false)
    var isRedeemable: Boolean = false,

    @Column(name = "is_convertible", nullable = false)
    var isConvertible: Boolean = false,

    @Column(name = "par_value", precision = 19, scale = 2)
    var parValue: BigDecimal? = null,

    @Column(name = "is_no_par_value", nullable = false)
    var isNoParValue: Boolean = false,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "NZD",

    // Voting Rights
    @Column(name = "voting_rights", nullable = false)
    var votingRights: String = "NONE", // NONE, ORDINARY, WEIGHTED, RESTRICTED

    @Column(name = "votes_per_share", nullable = false)
    var votesPerShare: Int = 0,

    @Column(name = "voting_restrictions", length = 1000)
    var votingRestrictions: String? = null,

    // Dividend Rights
    @Column(name = "dividend_rights", nullable = false)
    var dividendRights: String = "NONE", // NONE, ORDINARY, PREFERRED, CUMULATIVE

    @Column(name = "dividend_rate", precision = 5, scale = 4)
    var dividendRate: BigDecimal? = null, // For preferred shares (e.g., 0.05 = 5%)

    @Column(name = "is_cumulative_dividend", nullable = false)
    var isCumulativeDividend: Boolean = false,

    @Column(name = "dividend_priority", nullable = false)
    var dividendPriority: Int = 0, // 0 = lowest priority, higher numbers = higher priority

    // Capital Distribution Rights
    @Column(name = "capital_distribution_rights", nullable = false)
    var capitalDistributionRights: String = "ORDINARY", // ORDINARY, PREFERRED, NONE

    @Column(name = "liquidation_preference_multiple", precision = 5, scale = 2)
    var liquidationPreferenceMultiple: BigDecimal? = null, // e.g., 1.5x preference

    @Column(name = "liquidation_priority", nullable = false)
    var liquidationPriority: Int = 0,

    // Transfer Restrictions
    @Column(name = "is_transferable", nullable = false)
    var isTransferable: Boolean = true,

    @Column(name = "transfer_restrictions", length = 1000)
    var transferRestrictions: String? = null,

    @Column(name = "requires_board_approval", nullable = false)
    var requiresBoardApproval: Boolean = false,

    @Column(name = "has_preemptive_rights", nullable = false)
    var hasPreemptiveRights: Boolean = false,

    @Column(name = "has_tag_along_rights", nullable = false)
    var hasTagAlongRights: Boolean = false,

    @Column(name = "has_drag_along_rights", nullable = false)
    var hasDragAlongRights: Boolean = false,

    // Administrative
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    /**
     * Check if this share class has voting rights
     */
    fun hasVotingRights(): Boolean {
        return votingRights != "NONE" && votesPerShare > 0
    }

    /**
     * Check if this share class has dividend rights
     */
    fun hasDividendRights(): Boolean {
        return dividendRights != "NONE"
    }

    /**
     * Check if this share class has liquidation preference
     */
    fun hasLiquidationPreference(): Boolean {
        return capitalDistributionRights == "PREFERRED" && liquidationPreferenceMultiple != null
    }

    /**
     * Get effective votes per share (0 if no voting rights)
     */
    fun getEffectiveVotesPerShare(): Int {
        return if (hasVotingRights()) votesPerShare else 0
    }

    /**
     * Check if shares can be transferred freely
     */
    fun canTransferFreely(): Boolean {
        return isTransferable && !requiresBoardApproval && transferRestrictions.isNullOrBlank()
    }

    /**
     * Get display name for the share class
     */
    fun getDisplayName(): String {
        return "$className ($classCode)"
    }

    /**
     * Get a summary of key rights
     */
    fun getRightsSummary(): String {
        val rights = mutableListOf<String>()

        if (hasVotingRights()) {
            rights.add("$votesPerShare vote${if (votesPerShare != 1) "s" else ""} per share")
        }

        if (hasDividendRights()) {
            when (dividendRights) {
                "PREFERRED" -> {
                    val rate = dividendRate?.multiply(BigDecimal(100)) ?: BigDecimal.ZERO
                    rights.add("$rate% preferred dividend")
                }
                "CUMULATIVE" -> rights.add("Cumulative dividend")
                "ORDINARY" -> rights.add("Ordinary dividend")
            }
        }

        if (hasLiquidationPreference()) {
            rights.add("${liquidationPreferenceMultiple}x liquidation preference")
        }

        if (isRedeemable) rights.add("Redeemable")
        if (isConvertible) rights.add("Convertible")

        return if (rights.isEmpty()) "No special rights" else rights.joinToString(", ")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShareClass) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "ShareClass(id=$id, className='$className', classCode='$classCode', company=${company.companyName})"
    }
}
