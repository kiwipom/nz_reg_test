package nz.govt.companiesoffice.register.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "addresses")
class Address(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false)
    val addressType: AddressType,

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

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "phone")
    var phone: String? = null,

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDate,

    @Column(name = "effective_to")
    var effectiveTo: LocalDate? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun isEffective(date: LocalDate = LocalDate.now()): Boolean {
        return date >= effectiveFrom && (effectiveTo == null || date <= effectiveTo)
    }

    fun getFullAddress(): String {
        val parts = listOfNotNull(
            addressLine1,
            addressLine2,
            city,
            region,
            postcode,
            if (country != "NZ") country else null
        )
        return parts.joinToString(", ")
    }

    fun isSameAddress(other: Address): Boolean {
        return addressLine1 == other.addressLine1 &&
               addressLine2 == other.addressLine2 &&
               city == other.city &&
               region == other.region &&
               postcode == other.postcode &&
               country == other.country
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Address) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Address(id=$id, type=$addressType, address='$addressLine1, $city')"
    }
}

enum class AddressType {
    REGISTERED, SERVICE, COMMUNICATION
}