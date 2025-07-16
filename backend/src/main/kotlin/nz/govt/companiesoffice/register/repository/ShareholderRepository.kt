package nz.govt.companiesoffice.register.repository

import nz.govt.companiesoffice.register.entity.Shareholder
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ShareholderRepository : JpaRepository<Shareholder, Long> {

    // Find shareholders by company
    fun findByCompanyId(companyId: Long): List<Shareholder>

    // Find shareholders by name (case-insensitive)
    fun findByFullNameContainingIgnoreCase(fullName: String): List<Shareholder>

    // Check if shareholder exists for company
    fun existsByCompanyIdAndFullNameIgnoreCase(companyId: Long, fullName: String): Boolean

    // Find individual shareholders only
    fun findByCompanyIdAndIsIndividualTrue(companyId: Long): List<Shareholder>

    // Find corporate shareholders only
    fun findByCompanyIdAndIsIndividualFalse(companyId: Long): List<Shareholder>

    // Find shareholders by location
    fun findByCompanyIdAndCityIgnoreCaseAndCountryIgnoreCase(
        companyId: Long,
        city: String,
        country: String,
    ): List<Shareholder>

    // Find shareholders by country
    fun findByCompanyIdAndCountryIgnoreCase(companyId: Long, country: String): List<Shareholder>

    // Count shareholders for a company
    @Query("SELECT COUNT(s) FROM Shareholder s WHERE s.company.id = :companyId")
    fun countShareholdersByCompanyId(@Param("companyId") companyId: Long): Long

    // Count individual vs corporate shareholders
    @Query("SELECT COUNT(s) FROM Shareholder s WHERE s.company.id = :companyId AND s.isIndividual = :isIndividual")
    fun countShareholdersByType(@Param("companyId") companyId: Long, @Param("isIndividual") isIndividual: Boolean): Long

    // Search shareholders across all companies
    @Query(
        """
        SELECT s FROM Shareholder s 
        WHERE LOWER(s.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(s.city) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(s.country) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """,
    )
    fun searchShareholders(@Param("searchTerm") searchTerm: String): List<Shareholder>

    // Find shareholders by address components
    fun findByAddressLine1ContainingIgnoreCase(addressLine1: String): List<Shareholder>

    // Find shareholders in specific region
    fun findByCompanyIdAndRegionIgnoreCase(companyId: Long, region: String): List<Shareholder>

    // Find shareholders by postcode
    fun findByCompanyIdAndPostcode(companyId: Long, postcode: String): List<Shareholder>

    // Get shareholder statistics for a company
    @Query(
        """
        SELECT new map(
            'totalShareholders' as key, COUNT(s) as value
        ) FROM Shareholder s 
        WHERE s.company.id = :companyId
        GROUP BY s.company.id
    """,
    )
    fun getShareholderStatistics(@Param("companyId") companyId: Long): Map<String, Long>

    // Find shareholders by partial address match
    @Query(
        """
        SELECT s FROM Shareholder s 
        WHERE s.company.id = :companyId 
        AND (
            LOWER(s.addressLine1) LIKE LOWER(CONCAT('%', :addressTerm, '%'))
            OR LOWER(s.addressLine2) LIKE LOWER(CONCAT('%', :addressTerm, '%'))
            OR LOWER(s.city) LIKE LOWER(CONCAT('%', :addressTerm, '%'))
            OR LOWER(s.region) LIKE LOWER(CONCAT('%', :addressTerm, '%'))
        )
    """,
    )
    fun findByAddressContaining(
        @Param("companyId") companyId: Long,
        @Param("addressTerm") addressTerm: String,
    ): List<Shareholder>
}
