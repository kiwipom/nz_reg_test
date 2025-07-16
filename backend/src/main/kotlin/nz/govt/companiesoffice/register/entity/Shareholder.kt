package nz.govt.companiesoffice.register.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
@Table(name = "shareholders")
class Shareholder(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,

    @Column(name = "shareholder_name", nullable = false)
    var shareholderName: String,

    @Column(name = "shareholder_address", nullable = false)
    var shareholderAddress: String,

    @Column(name = "number_of_shares", nullable = false)
    var numberOfShares: BigDecimal,

    @Column(name = "share_class", nullable = false)
    var shareClass: String = "ORDINARY",

    @Enumerated(EnumType.STRING)
    @Column(name = "shareholder_type", nullable = false)
    var shareholderType: ShareholderType,

    @Column(name = "holding_date", nullable = false)
    var holdingDate: LocalDate,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun getSharePercentage(totalShares: BigDecimal): BigDecimal {
        return if (totalShares > BigDecimal.ZERO) {
            numberOfShares.divide(totalShares, 4, BigDecimal.ROUND_HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shareholder) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Shareholder(id=$id, name='$shareholderName', shares=$numberOfShares, type=$shareholderType)"
    }
}

enum class ShareholderType {
    INDIVIDUAL, COMPANY, TRUST, PARTNERSHIP
}