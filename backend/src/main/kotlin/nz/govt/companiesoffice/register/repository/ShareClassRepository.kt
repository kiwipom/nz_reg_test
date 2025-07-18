package nz.govt.companiesoffice.register.repository

import nz.govt.companiesoffice.register.entity.ShareClass
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ShareClassRepository : JpaRepository<ShareClass, Long> {

    /**
     * Find all active share classes for a company
     */
    fun findByCompany_IdAndIsActiveTrue(companyId: Long): List<ShareClass>

    /**
     * Find all share classes for a company (including inactive)
     */
    fun findByCompany_IdOrderByCreatedAtAsc(companyId: Long): List<ShareClass>

    /**
     * Find share class by company ID and class code
     */
    fun findByCompany_IdAndClassCode(companyId: Long, classCode: String): ShareClass?

    /**
     * Find share class by company ID and class name
     */
    fun findByCompany_IdAndClassName(companyId: Long, className: String): ShareClass?

    /**
     * Check if a class code exists for a company
     */
    fun existsByCompany_IdAndClassCode(companyId: Long, classCode: String): Boolean

    /**
     * Check if a class name exists for a company
     */
    fun existsByCompany_IdAndClassName(companyId: Long, className: String): Boolean

    /**
     * Find share classes with voting rights for a company
     */
    @Query(
        """
        SELECT sc FROM ShareClass sc 
        WHERE sc.company.id = :companyId 
        AND sc.votingRights != 'NONE' 
        AND sc.isActive = true
        """,
    )
    fun findVotingShareClassesByCompanyId(@Param("companyId") companyId: Long): List<ShareClass>

    /**
     * Find share classes with dividend rights for a company
     */
    @Query(
        """
        SELECT sc FROM ShareClass sc 
        WHERE sc.company.id = :companyId 
        AND sc.dividendRights != 'NONE' 
        AND sc.isActive = true
        """,
    )
    fun findDividendShareClassesByCompanyId(@Param("companyId") companyId: Long): List<ShareClass>

    /**
     * Find preferred share classes for a company
     */
    @Query(
        """
        SELECT sc FROM ShareClass sc 
        WHERE sc.company.id = :companyId 
        AND sc.dividendRights = 'PREFERRED' 
        AND sc.isActive = true 
        ORDER BY sc.dividendPriority DESC
        """,
    )
    fun findPreferredShareClassesByCompanyId(@Param("companyId") companyId: Long): List<ShareClass>

    /**
     * Find share classes with liquidation preference for a company
     */
    @Query(
        """
        SELECT sc FROM ShareClass sc 
        WHERE sc.company.id = :companyId 
        AND sc.capitalDistributionRights = 'PREFERRED' 
        AND sc.isActive = true 
        ORDER BY sc.liquidationPriority DESC
        """,
    )
    fun findLiquidationPreferenceShareClassesByCompanyId(@Param("companyId") companyId: Long): List<ShareClass>

    /**
     * Find redeemable share classes for a company
     */
    fun findByCompany_IdAndIsRedeemableTrueAndIsActiveTrue(companyId: Long): List<ShareClass>

    /**
     * Find convertible share classes for a company
     */
    fun findByCompany_IdAndIsConvertibleTrueAndIsActiveTrue(companyId: Long): List<ShareClass>

    /**
     * Get share class statistics for a company
     */
    @Query(
        """
        SELECT sc.id, sc.className, sc.classCode, 
               COUNT(sa.id) as allocationCount, 
               COALESCE(SUM(sa.numberOfShares), 0) as totalShares,
               COALESCE(SUM(sa.numberOfShares * sa.nominalValue), 0) as totalValue
        FROM ShareClass sc 
        LEFT JOIN ShareAllocation sa ON sa.shareClass = sc.classCode AND sa.company.id = sc.company.id AND sa.status = 'ACTIVE'
        WHERE sc.company.id = :companyId AND sc.isActive = true
        GROUP BY sc.id, sc.className, sc.classCode
        ORDER BY sc.createdAt
    """,
    )
    fun getShareClassStatistics(@Param("companyId") companyId: Long): List<Array<Any>>

    /**
     * Count active share classes for a company
     */
    fun countByCompany_IdAndIsActiveTrue(companyId: Long): Long

    /**
     * Find share classes that require board approval for transfers
     */
    fun findByCompany_IdAndRequiresBoardApprovalTrueAndIsActiveTrue(companyId: Long): List<ShareClass>
}
