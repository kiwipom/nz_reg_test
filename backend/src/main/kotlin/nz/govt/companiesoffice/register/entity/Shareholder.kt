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

    @Column(name = "full_name", nullable = false)
    var fullName: String,

    @Column(name = "address_line_1", nullable = false)
    var addressLine1: String,

    @Column(name = "address_line_2")
    var addressLine2: String? = null,

    @Column(name = "city", nullable = false)
    var city: String,

    @Column(name = "region")
    var region: String? = null,

    @Column(name = "postcode")
    var postcode: String? = null,

    @Column(name = "country", nullable = false)
    var country: String = "NZ",

    @Column(name = "is_individual", nullable = false)
    var isIndividual: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun getFullAddress(): String {
        val parts = listOfNotNull(
            addressLine1,
            addressLine2,
            city,
            region,
            postcode,
            if (country != "NZ") country else null,
        )
        return parts.joinToString(", ")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shareholder) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Shareholder(id=$id, name='$fullName', individual=$isIndividual)"
    }
}

// Note: ShareholderType moved to isIndividual boolean field to match database schema