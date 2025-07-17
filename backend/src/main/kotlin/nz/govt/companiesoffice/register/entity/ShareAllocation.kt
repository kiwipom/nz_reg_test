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
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "share_allocations")
class ShareAllocation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shareholder_id", nullable = false)
    var shareholder: Shareholder,

    @Column(name = "share_class", nullable = false)
    var shareClass: String = "Ordinary",

    @Column(name = "number_of_shares", nullable = false)
    var numberOfShares: Long,

    @Column(name = "nominal_value", nullable = false, precision = 19, scale = 2)
    var nominalValue: BigDecimal,

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
    var amountPaid: BigDecimal,

    @Column(name = "allocation_date", nullable = false)
    var allocationDate: LocalDate,

    @Column(name = "transfer_date")
    var transferDate: LocalDate? = null,

    @Column(name = "transfer_to_shareholder_id")
    var transferToShareholderId: Long? = null,

    @Column(name = "status", nullable = false)
    var status: String = "ACTIVE", // ACTIVE, TRANSFERRED, CANCELLED

    @Column(name = "certificate_number")
    var certificateNumber: String? = null,

    @Column(name = "is_fully_paid", nullable = false)
    var isFullyPaid: Boolean = false,

    @Column(name = "restrictions")
    var restrictions: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun getUnpaidAmount(): BigDecimal {
        return nominalValue.multiply(BigDecimal(numberOfShares)).subtract(amountPaid)
    }

    fun getTotalValue(): BigDecimal {
        return nominalValue.multiply(BigDecimal(numberOfShares))
    }

    fun isActiveAllocation(): Boolean {
        return status == "ACTIVE"
    }

    fun isTransferred(): Boolean {
        return status == "TRANSFERRED"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShareAllocation) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "ShareAllocation(id=$id, shareClass='$shareClass', shares=$numberOfShares, status='$status')"
    }
}
