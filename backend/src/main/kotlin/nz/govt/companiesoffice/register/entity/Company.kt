package nz.govt.companiesoffice.register.entity

import jakarta.persistence.*
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

    @Column(name = "company_name", nullable = false)
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
    var version: Int = 1
) {
    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val addresses: MutableList<Address> = mutableListOf()

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val directors: MutableList<Director> = mutableListOf()

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val shareholders: MutableList<Shareholder> = mutableListOf()

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val shareAllocations: MutableList<ShareAllocation> = mutableListOf()

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val annualReturns: MutableList<AnnualReturn> = mutableListOf()

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val documents: MutableList<Document> = mutableListOf()

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

    fun getActiveDirectors(): List<Director> {
        return directors.filter { it.status == DirectorStatus.ACTIVE }
    }

    fun getResidentDirectors(): List<Director> {
        return getActiveDirectors().filter { it.isNzResident || it.isAustralianResident }
    }

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