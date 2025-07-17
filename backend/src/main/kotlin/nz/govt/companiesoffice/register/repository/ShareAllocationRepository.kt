package nz.govt.companiesoffice.register.repository

import nz.govt.companiesoffice.register.entity.ShareAllocation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface ShareAllocationRepository : JpaRepository<ShareAllocation, Long> {

    // Find all active allocations for a company
    fun findByCompanyIdAndStatus(companyId: Long, status: String): List<ShareAllocation>

    // Find all allocations for a shareholder
    fun findByShareholderId(shareholderId: Long): List<ShareAllocation>

    // Find active allocations for a shareholder
    fun findByShareholderIdAndStatus(shareholderId: Long, status: String): List<ShareAllocation>

    // Find allocations by share class
    fun findByCompanyIdAndShareClass(companyId: Long, shareClass: String): List<ShareAllocation>

    // Find allocations by share class and status
    fun findByCompanyIdAndShareClassAndStatus(
        companyId: Long,
        shareClass: String,
        status: String,
    ): List<ShareAllocation>

    // Count total active shares for a company
    @Query(
        """
        SELECT COALESCE(SUM(sa.numberOfShares), 0) 
        FROM ShareAllocation sa 
        WHERE sa.company.id = :companyId AND sa.status = 'ACTIVE'
        """,
    )
    fun getTotalActiveShares(@Param("companyId") companyId: Long): Long

    // Count total active shares by share class
    @Query(
        """
        SELECT COALESCE(SUM(sa.numberOfShares), 0) 
        FROM ShareAllocation sa 
        WHERE sa.company.id = :companyId 
        AND sa.shareClass = :shareClass 
        AND sa.status = 'ACTIVE'
        """,
    )
    fun getTotalActiveSharesByClass(
        @Param("companyId") companyId: Long,
        @Param("shareClass") shareClass: String,
    ): Long

    // Get total value of active shares
    @Query(
        """
        SELECT COALESCE(SUM(sa.numberOfShares * sa.nominalValue), 0) 
        FROM ShareAllocation sa 
        WHERE sa.company.id = :companyId AND sa.status = 'ACTIVE'
        """,
    )
    fun getTotalActiveShareValue(@Param("companyId") companyId: Long): BigDecimal

    // Get total amount paid for active shares
    @Query(
        """
        SELECT COALESCE(SUM(sa.amountPaid), 0) 
        FROM ShareAllocation sa 
        WHERE sa.company.id = :companyId AND sa.status = 'ACTIVE'
        """,
    )
    fun getTotalPaidAmount(@Param("companyId") companyId: Long): BigDecimal

    // Find allocations by date range
    fun findByCompanyIdAndAllocationDateBetween(
        companyId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ShareAllocation>

    // Find transfers in date range
    fun findByCompanyIdAndTransferDateBetween(
        companyId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ShareAllocation>

    // Find unpaid allocations
    fun findByCompanyIdAndIsFullyPaidFalse(companyId: Long): List<ShareAllocation>

    // Find allocations with restrictions
    fun findByCompanyIdAndRestrictionsIsNotNull(companyId: Long): List<ShareAllocation>

    // Get share class statistics
    @Query(
        """
        SELECT new map(
            sa.shareClass as shareClass,
            COUNT(sa) as allocationCount,
            SUM(sa.numberOfShares) as totalShares,
            SUM(sa.numberOfShares * sa.nominalValue) as totalValue,
            SUM(sa.amountPaid) as totalPaid
        )
        FROM ShareAllocation sa 
        WHERE sa.company.id = :companyId AND sa.status = 'ACTIVE'
        GROUP BY sa.shareClass
        """,
    )
    fun getShareClassStatistics(@Param("companyId") companyId: Long): List<Map<String, Any>>

    // Get shareholder ownership summary
    @Query(
        """
        SELECT new map(
            sa.shareholder.id as shareholderId,
            sa.shareholder.fullName as shareholderName,
            COUNT(sa) as allocationCount,
            SUM(sa.numberOfShares) as totalShares,
            SUM(sa.numberOfShares * sa.nominalValue) as totalValue,
            SUM(sa.amountPaid) as totalPaid
        )
        FROM ShareAllocation sa 
        WHERE sa.company.id = :companyId AND sa.status = 'ACTIVE'
        GROUP BY sa.shareholder.id, sa.shareholder.fullName
        ORDER BY SUM(sa.numberOfShares) DESC
        """,
    )
    fun getShareholderOwnershipSummary(@Param("companyId") companyId: Long): List<Map<String, Any>>

    // Find allocations by certificate number
    fun findByCertificateNumber(certificateNumber: String): List<ShareAllocation>

    // Check if shareholder has shares in specific class
    fun existsByShareholderIdAndShareClassAndStatus(
        shareholderId: Long,
        shareClass: String,
        status: String,
    ): Boolean

    // Find all distinct share classes for a company
    @Query(
        """
        SELECT DISTINCT sa.shareClass 
        FROM ShareAllocation sa 
        WHERE sa.company.id = :companyId
        ORDER BY sa.shareClass
        """,
    )
    fun getDistinctShareClasses(@Param("companyId") companyId: Long): List<String>
}
