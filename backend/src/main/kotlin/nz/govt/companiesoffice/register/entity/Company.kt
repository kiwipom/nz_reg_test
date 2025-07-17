package nz.govt.companiesoffice.register.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "companies")
class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "company_number", unique = true, nullable = false)
    val companyNumber: String,

    @Column(name = "company_name", nullable = false, unique = true)
    var companyName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false)
    val companyType: CompanyType,

    @Column(name = "incorporation_date", nullable = false)
    val incorporationDate: LocalDate,

    @Column(name = "nzbn", unique = true)
    var nzbn: String? = null,

    @Column(name = "status", nullable = false)
    var status: String = "ACTIVE",

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "version", nullable = false)
    @Version
    var version: Int = 1,
) {
    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val addresses: MutableList<Address> = mutableListOf()

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val directors: MutableList<Director> = mutableListOf()

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val shareholders: MutableList<Shareholder> = mutableListOf()

    // Note: Additional entities (shareAllocations, annualReturns, documents)
    // will be added in future phases

    fun addAddress(address: Address) {
        addresses.add(address)
        address.company = this
    }

    fun addDirector(director: Director) {
        directors.add(director)
        director.company = this
    }

    fun addShareholder(shareholder: Shareholder) {
        shareholders.add(shareholder)
        shareholder.company = this
    }

    @JsonIgnore
    fun getActiveDirectors(): List<Director> {
        return directors.filter { it.status == DirectorStatus.ACTIVE }
    }

    @JsonIgnore
    fun getResidentDirectors(): List<Director> {
        return getActiveDirectors().filter { it.isNzResident || it.isAustralianResident }
    }

    @JsonIgnore
    fun getCurrentAddress(addressType: AddressType): Address? {
        return addresses.find {
            it.addressType == addressType &&
                it.isEffective()
        }
    }

    fun isActive(): Boolean = status == "ACTIVE"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Company) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Company(id=$id, companyNumber='$companyNumber', companyName='$companyName', companyType=$companyType)"
    }
}

enum class CompanyType {
    LTD, OVERSEAS, UNLIMITED
}
