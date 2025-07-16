package nz.govt.companiesoffice.register.repository

import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.Document
import nz.govt.companiesoffice.register.entity.DocumentType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface DocumentRepository : JpaRepository<Document, Long> {

    fun findByCompanyAndDocumentType(company: Company, documentType: DocumentType): List<Document>

    fun findByCompanyIdAndDocumentType(companyId: Long, documentType: DocumentType): List<Document>

    fun findByCompanyOrderByUploadedAtDesc(company: Company): List<Document>

    fun findByCompanyIdOrderByUploadedAtDesc(companyId: Long): List<Document>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.company = :company 
        AND d.documentType = :documentType 
        ORDER BY d.versionNumber DESC, d.uploadedAt DESC
        """,
    )
    fun findLatestDocumentByCompanyAndType(
        @Param("company") company: Company,
        @Param("documentType") documentType: DocumentType,
    ): List<Document>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.company.id = :companyId 
        AND d.documentType = :documentType 
        ORDER BY d.versionNumber DESC, d.uploadedAt DESC
        """,
    )
    fun findLatestDocumentByCompanyIdAndType(
        @Param("companyId") companyId: Long,
        @Param("documentType") documentType: DocumentType,
    ): List<Document>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.company = :company 
        AND d.documentType = :documentType 
        AND d.versionNumber = :version
        """,
    )
    fun findDocumentByCompanyTypeAndVersion(
        @Param("company") company: Company,
        @Param("documentType") documentType: DocumentType,
        @Param("version") version: Int,
    ): Document?

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.isPublic = true 
        AND d.company = :company 
        ORDER BY d.uploadedAt DESC
        """,
    )
    fun findPublicDocumentsByCompany(@Param("company") company: Company): List<Document>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.isPublic = true 
        AND d.company.id = :companyId 
        ORDER BY d.uploadedAt DESC
        """,
    )
    fun findPublicDocumentsByCompanyId(@Param("companyId") companyId: Long): List<Document>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.retentionUntil IS NOT NULL 
        AND d.retentionUntil <= :date
        """,
    )
    fun findDocumentsForRetention(@Param("date") date: LocalDate = LocalDate.now()): List<Document>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.uploadedAt BETWEEN :startDate AND :endDate 
        ORDER BY d.uploadedAt DESC
        """,
    )
    fun findDocumentsByUploadDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): List<Document>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.uploadedBy = :uploadedBy 
        ORDER BY d.uploadedAt DESC
        """,
    )
    fun findDocumentsByUploadedBy(@Param("uploadedBy") uploadedBy: String): List<Document>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.mimeType = :mimeType 
        ORDER BY d.uploadedAt DESC
        """,
    )
    fun findDocumentsByMimeType(@Param("mimeType") mimeType: String): List<Document>

    @Query(
        """
        SELECT COALESCE(MAX(d.versionNumber), 0) 
        FROM Document d 
        WHERE d.company = :company 
        AND d.documentType = :documentType
        """,
    )
    fun findLatestVersionNumber(
        @Param("company") company: Company,
        @Param("documentType") documentType: DocumentType,
    ): Int

    @Query(
        """
        SELECT COUNT(d) 
        FROM Document d 
        WHERE d.company = :company 
        AND d.documentType = :documentType
        """,
    )
    fun countDocumentsByCompanyAndType(
        @Param("company") company: Company,
        @Param("documentType") documentType: DocumentType,
    ): Long

    @Query(
        """
        SELECT SUM(d.fileSize) 
        FROM Document d 
        WHERE d.company = :company 
        AND d.fileSize IS NOT NULL
        """,
    )
    fun getTotalFileSizeByCompany(@Param("company") company: Company): Long?

    @Query(
        """
        SELECT d.documentType, COUNT(d) 
        FROM Document d 
        WHERE d.company = :company 
        GROUP BY d.documentType
        """,
    )
    fun getDocumentCountByTypeForCompany(@Param("company") company: Company): List<Array<Any>>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE LOWER(d.documentName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
        OR LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY d.uploadedAt DESC
        """,
    )
    fun searchDocuments(@Param("searchTerm") searchTerm: String): List<Document>

    @Query(
        """
        SELECT d FROM Document d 
        WHERE d.company = :company 
        AND (LOWER(d.documentName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
        OR LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY d.uploadedAt DESC
        """,
    )
    fun searchDocumentsByCompany(
        @Param("company") company: Company,
        @Param("searchTerm") searchTerm: String,
    ): List<Document>
}
