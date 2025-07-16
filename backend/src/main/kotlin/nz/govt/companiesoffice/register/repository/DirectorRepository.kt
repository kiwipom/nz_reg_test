package nz.govt.companiesoffice.register.repository

import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.entity.DirectorStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DirectorRepository : JpaRepository<Director, Long> {

    // Find directors by company
    fun findByCompanyId(companyId: Long): List<Director>

    // Find active directors only
    fun findByCompanyIdAndStatus(companyId: Long, status: DirectorStatus): List<Director>

    // Find directors by name (case-insensitive)
    fun findByFullNameContainingIgnoreCase(fullName: String): List<Director>

    // Check if director exists for company
    fun existsByCompanyIdAndFullNameIgnoreCase(companyId: Long, fullName: String): Boolean

    // Find directors by residency status
    fun findByCompanyIdAndIsNzResidentTrueOrIsAustralianResidentTrue(companyId: Long): List<Director>

    // Find directors without consent
    fun findByCompanyIdAndConsentGivenFalse(companyId: Long): List<Director>

    // Find directors appointed between dates
    fun findByCompanyIdAndAppointedDateBetween(
        companyId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Director>

    // Count active directors for a company
    @Query("SELECT COUNT(d) FROM Director d WHERE d.company.id = :companyId AND d.status = 'ACTIVE'")
    fun countActiveDirectorsByCompanyId(@Param("companyId") companyId: Long): Long

    // Check NZ residency requirements compliance
    @Query(
        """
        SELECT COUNT(d) FROM Director d 
        WHERE d.company.id = :companyId 
        AND d.status = 'ACTIVE' 
        AND (d.isNzResident = true OR d.isAustralianResident = true)
    """,
    )
    fun countResidentDirectorsByCompanyId(@Param("companyId") companyId: Long): Long

    // Find directors requiring consent
    @Query(
        """
        SELECT d FROM Director d 
        WHERE d.company.id = :companyId 
        AND d.consentGiven = false 
        AND d.status = 'ACTIVE'
    """,
    )
    fun findDirectorsRequiringConsent(@Param("companyId") companyId: Long): List<Director>

    // Search directors across all companies
    @Query(
        """
        SELECT d FROM Director d 
        WHERE LOWER(d.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(d.residentialCity) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(d.residentialCountry) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """,
    )
    fun searchDirectors(@Param("searchTerm") searchTerm: String): List<Director>

    // Find directors by birth place
    fun findByPlaceOfBirthContainingIgnoreCase(placeOfBirth: String): List<Director>

    // Find directors by residential location
    fun findByResidentialCityIgnoreCaseAndResidentialCountryIgnoreCase(
        city: String,
        country: String,
    ): List<Director>

    // Find directors that resigned on or after a date
    fun findByResignedDateGreaterThanEqual(resignedDate: LocalDate): List<Director>

    // Find directors appointed in the last N days
    @Query(
        """
        SELECT d FROM Director d 
        WHERE d.appointedDate >= :fromDate 
        AND d.company.id = :companyId
    """,
    )
    fun findRecentlyAppointedDirectors(
        @Param("companyId") companyId: Long,
        @Param("fromDate") fromDate: LocalDate,
    ): List<Director>
}
