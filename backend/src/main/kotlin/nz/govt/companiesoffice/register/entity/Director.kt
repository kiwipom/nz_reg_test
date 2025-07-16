package nz.govt.companiesoffice.register.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "directors")
class Director(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,

    @Column(name = "full_name", nullable = false)
    var fullName: String,

    @Column(name = "date_of_birth")
    var dateOfBirth: LocalDate? = null,

    @Column(name = "place_of_birth")
    var placeOfBirth: String? = null,

    @Column(name = "residential_address_line_1", nullable = false)
    var residentialAddressLine1: String,

    @Column(name = "residential_address_line_2")
    var residentialAddressLine2: String? = null,

    @Column(name = "residential_city", nullable = false)
    var residentialCity: String,

    @Column(name = "residential_region")
    var residentialRegion: String? = null,

    @Column(name = "residential_postcode")
    var residentialPostcode: String? = null,

    @Column(name = "residential_country", nullable = false)
    var residentialCountry: String = "NZ",

    @Column(name = "is_nz_resident", nullable = false)
    var isNzResident: Boolean,

    @Column(name = "is_australian_resident", nullable = false)
    var isAustralianResident: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: DirectorStatus = DirectorStatus.ACTIVE,

    @Column(name = "consent_given", nullable = false)
    var consentGiven: Boolean = false,

    @Column(name = "consent_date")
    var consentDate: LocalDate? = null,

    @Column(name = "appointed_date", nullable = false)
    var appointedDate: LocalDate,

    @Column(name = "resigned_date")
    var resignedDate: LocalDate? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun isActive(): Boolean = status == DirectorStatus.ACTIVE

    fun isResident(): Boolean = isNzResident || isAustralianResident

    fun getResidentialAddress(): String {
        val parts = listOfNotNull(
            residentialAddressLine1,
            residentialAddressLine2,
            residentialCity,
            residentialRegion,
            residentialPostcode,
            if (residentialCountry != "NZ") residentialCountry else null
        )
        return parts.joinToString(", ")
    }

    fun resign(resignationDate: LocalDate = LocalDate.now()) {
        if (status == DirectorStatus.ACTIVE) {
            status = DirectorStatus.RESIGNED
            resignedDate = resignationDate
        }
    }

    fun giveConsent(consentDate: LocalDate = LocalDate.now()) {
        this.consentGiven = true
        this.consentDate = consentDate
    }

    fun validateResidencyRequirement() {
        if (!isNzResident && !isAustralianResident) {
            throw IllegalStateException("Director must be NZ or Australian resident")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Director) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Director(id=$id, fullName='$fullName', status=$status, appointed=$appointedDate)"
    }
}

enum class DirectorStatus {
    ACTIVE, RESIGNED, DISQUALIFIED
}