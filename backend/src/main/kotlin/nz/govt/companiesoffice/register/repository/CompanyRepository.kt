package nz.govt.companiesoffice.register.repository

import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface CompanyRepository : JpaRepository<Company, Long> {
    
    fun findByCompanyNumber(companyNumber: String): Company?
    
    fun findByCompanyNameContainingIgnoreCase(companyName: String): List<Company>
    
    fun findByStatus(status: String): List<Company>
    
    fun findByCompanyType(companyType: CompanyType): List<Company>
    
    fun findByIncorporationDateBetween(startDate: LocalDate, endDate: LocalDate): List<Company>
    
    @Query("SELECT c FROM Company c WHERE c.status = 'ACTIVE' AND c.companyName LIKE %:searchTerm%")
    fun searchActiveCompanies(@Param("searchTerm") searchTerm: String): List<Company>
    
    @Query("SELECT c FROM Company c JOIN c.directors d WHERE d.fullName LIKE %:directorName% AND d.status = 'ACTIVE'")
    fun findByDirectorName(@Param("directorName") directorName: String): List<Company>
    
    @Query("SELECT c FROM Company c WHERE c.nzbn = :nzbn")
    fun findByNzbn(@Param("nzbn") nzbn: String): Company?
    
    @Query("""
        SELECT c FROM Company c 
        WHERE c.status = 'ACTIVE' 
        AND (
            LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR 
            c.companyNumber LIKE CONCAT('%', :query, '%')
        )
    """)
    fun searchCompanies(@Param("query") query: String): List<Company>
    
    fun existsByCompanyNumber(companyNumber: String): Boolean
    
    fun existsByCompanyNameIgnoreCase(companyName: String): Boolean
}