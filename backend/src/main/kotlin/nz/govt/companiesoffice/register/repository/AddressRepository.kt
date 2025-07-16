package nz.govt.companiesoffice.register.repository

import nz.govt.companiesoffice.register.entity.Address
import nz.govt.companiesoffice.register.entity.AddressType
import nz.govt.companiesoffice.register.entity.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AddressRepository : JpaRepository<Address, Long> {

    fun findByCompanyAndAddressType(company: Company, addressType: AddressType): List<Address>

    fun findByCompanyIdAndAddressType(companyId: Long, addressType: AddressType): List<Address>

    @Query(
        """
        SELECT a FROM Address a 
        WHERE a.company = :company 
        AND a.addressType = :addressType 
        AND a.effectiveFrom <= :date 
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        """,
    )
    fun findEffectiveAddressByCompanyAndType(
        @Param("company") company: Company,
        @Param("addressType") addressType: AddressType,
        @Param("date") date: LocalDate = LocalDate.now(),
    ): Address?

    @Query(
        """
        SELECT a FROM Address a 
        WHERE a.company.id = :companyId 
        AND a.addressType = :addressType 
        AND a.effectiveFrom <= :date 
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        """,
    )
    fun findEffectiveAddressByCompanyIdAndType(
        @Param("companyId") companyId: Long,
        @Param("addressType") addressType: AddressType,
        @Param("date") date: LocalDate = LocalDate.now(),
    ): Address?

    @Query(
        """
        SELECT a FROM Address a 
        WHERE a.company = :company 
        AND a.effectiveFrom <= :date 
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        """,
    )
    fun findEffectiveAddressesByCompany(
        @Param("company") company: Company,
        @Param("date") date: LocalDate = LocalDate.now(),
    ): List<Address>

    @Query(
        """
        SELECT a FROM Address a 
        WHERE a.company.id = :companyId 
        AND a.effectiveFrom <= :date 
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        """,
    )
    fun findEffectiveAddressesByCompanyId(
        @Param("companyId") companyId: Long,
        @Param("date") date: LocalDate = LocalDate.now(),
    ): List<Address>

    @Query(
        """
        SELECT a FROM Address a 
        WHERE a.company = :company 
        AND a.addressType = :addressType 
        ORDER BY a.effectiveFrom DESC
        """,
    )
    fun findAddressHistoryByCompanyAndType(
        @Param("company") company: Company,
        @Param("addressType") addressType: AddressType,
    ): List<Address>

    @Query(
        """
        SELECT a FROM Address a 
        WHERE a.company = :company 
        ORDER BY a.addressType ASC, a.effectiveFrom DESC
        """,
    )
    fun findAddressHistoryByCompany(@Param("company") company: Company): List<Address>

    @Query(
        """
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END 
        FROM Address a 
        WHERE a.company = :company 
        AND a.addressType = :addressType 
        AND a.effectiveFrom <= :toDate 
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :fromDate)
        AND a.id != :excludeId
        """,
    )
    fun hasOverlappingAddresses(
        @Param("company") company: Company,
        @Param("addressType") addressType: AddressType,
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate,
        @Param("excludeId") excludeId: Long = 0,
    ): Boolean

    @Query(
        """
        SELECT a FROM Address a 
        WHERE LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')) 
        AND a.effectiveFrom <= :date 
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        """,
    )
    fun findEffectiveAddressesByCity(
        @Param("city") city: String,
        @Param("date") date: LocalDate = LocalDate.now(),
    ): List<Address>

    @Query(
        """
        SELECT a FROM Address a 
        WHERE a.postcode = :postcode 
        AND a.effectiveFrom <= :date 
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        """,
    )
    fun findEffectiveAddressesByPostcode(
        @Param("postcode") postcode: String,
        @Param("date") date: LocalDate = LocalDate.now(),
    ): List<Address>

    @Query(
        """
        SELECT DISTINCT a.city FROM Address a 
        WHERE a.country = :country 
        AND a.effectiveFrom <= :date 
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        ORDER BY a.city ASC
        """,
    )
    fun findDistinctCitiesByCountry(
        @Param("country") country: String = "NZ",
        @Param("date") date: LocalDate = LocalDate.now(),
    ): List<String>

    @Query(
        """
        SELECT DISTINCT a.postcode FROM Address a 
        WHERE a.country = :country 
        AND a.postcode IS NOT NULL 
        AND a.effectiveFrom <= :date 
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
        ORDER BY a.postcode ASC
        """,
    )
    fun findDistinctPostcodesByCountry(
        @Param("country") country: String = "NZ",
        @Param("date") date: LocalDate = LocalDate.now(),
    ): List<String>
}
