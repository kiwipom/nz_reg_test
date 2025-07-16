package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.Document
import nz.govt.companiesoffice.register.entity.DocumentType
import nz.govt.companiesoffice.register.repository.DocumentRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val auditService: AuditService,
    private val s3Service: S3DocumentStorageService,
) {

    private val logger = LoggerFactory.getLogger(DocumentService::class.java)

    @Value("\${app.documents.max-file-size:10485760}") // 10MB default
    private val maxFileSize: Long = 10485760

    @Value("\${app.documents.allowed-types:application/pdf,image/jpeg,image/png,text/plain}")
    private val allowedMimeTypes: String = "application/pdf,image/jpeg,image/png,text/plain"

    data class DocumentUploadResult(
        val document: Document,
        val success: Boolean,
        val message: String,
    )

    fun uploadDocument(
        company: Company,
        documentType: DocumentType,
        file: MultipartFile,
        uploadedBy: String? = null,
        description: String? = null,
        isPublic: Boolean = true,
        retentionUntil: LocalDate? = null,
    ): DocumentUploadResult {
        logger.info("Uploading document of type ${documentType.name} for company ${company.id}")

        try {
            // Validate file
            validateFile(file)

            // Generate unique file path
            val fileName = generateFileName(company, documentType, file.originalFilename)
            val filePath = "companies/${company.id}/documents/${documentType.name.lowercase()}/$fileName"

            // Calculate checksum
            val checksum = calculateChecksum(file.bytes)

            // Get next version number
            val versionNumber = documentRepository.findLatestVersionNumber(company, documentType) + 1

            // Upload to S3
            val s3Key = s3Service.uploadFile(filePath, file.bytes, file.contentType ?: "application/octet-stream")

            // Create document entity
            val document = Document(
                company = company,
                documentType = documentType,
                documentName = file.originalFilename ?: "unknown",
                filePath = s3Key,
                fileSize = file.size,
                mimeType = file.contentType,
                uploadedBy = uploadedBy,
                isPublic = isPublic,
                retentionUntil = retentionUntil,
                versionNumber = versionNumber,
                description = description,
                checksum = checksum,
            )

            val savedDocument = documentRepository.save(document)

            auditService.logEvent(
                action = nz.govt.companiesoffice.register.audit.AuditAction.CREATE,
                resourceType = "Document",
                resourceId = savedDocument.id.toString(),
                details = mapOf(
                    "companyId" to company.id,
                    "documentType" to documentType.name,
                    "fileName" to (file.originalFilename ?: "unknown"),
                    "fileSize" to file.size,
                    "versionNumber" to versionNumber,
                    "uploadedBy" to uploadedBy,
                    "isPublic" to isPublic,
                ),
            )

            logger.info("Successfully uploaded document ${savedDocument.id} for company ${company.id}")

            return DocumentUploadResult(
                document = savedDocument,
                success = true,
                message = "Document uploaded successfully",
            )
        } catch (e: Exception) {
            logger.error("Failed to upload document for company ${company.id}", e)

            return DocumentUploadResult(
                document = Document(
                    company = company,
                    documentType = documentType,
                    documentName = file.originalFilename ?: "unknown",
                    filePath = "",
                ),
                success = false,
                message = "Failed to upload document: ${e.message}",
            )
        }
    }

    fun downloadDocument(documentId: Long): ByteArray? {
        logger.info("Downloading document $documentId")

        val document = documentRepository.findById(documentId).orElse(null)
            ?: throw IllegalArgumentException("Document not found: $documentId")

        if (document.isRetentionExpired()) {
            throw IllegalStateException("Document has expired and cannot be downloaded")
        }

        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.READ,
            resourceType = "Document",
            resourceId = documentId.toString(),
            details = mapOf(
                "companyId" to document.company.id,
                "documentType" to document.documentType.name,
                "fileName" to document.documentName,
            ),
        )

        return s3Service.downloadFile(document.filePath)
    }

    fun deleteDocument(documentId: Long, deletedBy: String? = null) {
        logger.info("Deleting document $documentId")

        val document = documentRepository.findById(documentId).orElse(null)
            ?: throw IllegalArgumentException("Document not found: $documentId")

        // Delete from S3
        s3Service.deleteFile(document.filePath)

        // Delete from database
        documentRepository.delete(document)

        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.DELETE,
            resourceType = "Document",
            resourceId = documentId.toString(),
            details = mapOf(
                "companyId" to document.company.id,
                "documentType" to document.documentType.name,
                "fileName" to document.documentName,
                "deletedBy" to deletedBy,
            ),
        )

        logger.info("Successfully deleted document $documentId")
    }

    @Transactional(readOnly = true)
    fun getDocumentsByCompany(company: Company): List<Document> {
        return documentRepository.findByCompanyOrderByUploadedAtDesc(company)
    }

    @Transactional(readOnly = true)
    fun getDocumentsByCompanyAndType(company: Company, documentType: DocumentType): List<Document> {
        return documentRepository.findByCompanyAndDocumentType(company, documentType)
    }

    @Transactional(readOnly = true)
    fun getLatestDocumentByCompanyAndType(company: Company, documentType: DocumentType): Document? {
        return documentRepository.findLatestDocumentByCompanyAndType(company, documentType).firstOrNull()
    }

    @Transactional(readOnly = true)
    fun getPublicDocumentsByCompany(company: Company): List<Document> {
        return documentRepository.findPublicDocumentsByCompany(company)
    }

    @Transactional(readOnly = true)
    fun getDocumentById(documentId: Long): Document? {
        return documentRepository.findById(documentId).orElse(null)
    }

    @Transactional(readOnly = true)
    fun searchDocuments(searchTerm: String): List<Document> {
        return documentRepository.searchDocuments(searchTerm)
    }

    @Transactional(readOnly = true)
    fun searchDocumentsByCompany(company: Company, searchTerm: String): List<Document> {
        return documentRepository.searchDocumentsByCompany(company, searchTerm)
    }

    fun processRetentionCleanup(): Int {
        logger.info("Processing document retention cleanup")

        val expiredDocuments = documentRepository.findDocumentsForRetention()
        var deletedCount = 0

        for (document in expiredDocuments) {
            try {
                deleteDocument(document.id, "SYSTEM_RETENTION")
                deletedCount++
            } catch (e: Exception) {
                logger.error("Failed to delete expired document ${document.id}", e)
            }
        }

        logger.info("Deleted $deletedCount expired documents")
        return deletedCount
    }

    @Transactional(readOnly = true)
    fun getDocumentStatistics(company: Company): Map<String, Any> {
        val documentCounts = documentRepository.getDocumentCountByTypeForCompany(company)
        val totalSize = documentRepository.getTotalFileSizeByCompany(company) ?: 0L

        return mapOf(
            "totalDocuments" to documentCounts.sumOf { it[1] as Long },
            "totalSizeBytes" to totalSize,
            "totalSizeMB" to totalSize / (1024.0 * 1024.0),
            "documentsByType" to documentCounts.associate {
                (it[0] as DocumentType).name to it[1] as Long
            },
        )
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw IllegalArgumentException("File is empty")
        }

        if (file.size > maxFileSize) {
            throw IllegalArgumentException("File size exceeds maximum allowed size of ${maxFileSize / (1024 * 1024)}MB")
        }

        val allowedTypes = allowedMimeTypes.split(",").map { it.trim() }
        if (file.contentType !in allowedTypes) {
            throw IllegalArgumentException("File type not allowed. Allowed types: $allowedMimeTypes")
        }

        // Check for malicious file extensions
        val fileName = file.originalFilename?.lowercase() ?: ""
        val dangerousExtensions = listOf(".exe", ".bat", ".cmd", ".com", ".pif", ".scr", ".vbs", ".js")
        if (dangerousExtensions.any { fileName.endsWith(it) }) {
            throw IllegalArgumentException("File type not allowed for security reasons")
        }
    }

    private fun generateFileName(company: Company, documentType: DocumentType, originalFilename: String?): String {
        val timestamp = LocalDateTime.now().toString().replace(":", "-")
        val extension = originalFilename?.substringAfterLast(".", "") ?: "unknown"
        val uuid = UUID.randomUUID().toString().substring(0, 8)

        return "${company.id}_${documentType.name}_${timestamp}_$uuid.$extension"
    }

    private fun calculateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
