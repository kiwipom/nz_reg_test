package nz.govt.companiesoffice.register.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "documents")
class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    val documentType: DocumentType,

    @Column(name = "document_name", nullable = false)
    var documentName: String,

    @Column(name = "file_path", nullable = false)
    var filePath: String,

    @Column(name = "file_size")
    var fileSize: Long? = null,

    @Column(name = "mime_type")
    var mimeType: String? = null,

    @Column(name = "uploaded_by")
    var uploadedBy: String? = null,

    @Column(name = "uploaded_at", nullable = false)
    val uploadedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = true,

    @Column(name = "retention_until")
    var retentionUntil: LocalDate? = null,

    @Column(name = "version_number", nullable = false)
    var versionNumber: Int = 1,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "checksum")
    var checksum: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    fun isRetentionExpired(): Boolean {
        return retentionUntil != null && LocalDate.now().isAfter(retentionUntil)
    }

    fun getFileExtension(): String? {
        return filePath.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
    }

    fun getFileSizeInMB(): Double? {
        return fileSize?.let { it / (1024.0 * 1024.0) }
    }

    fun isImageFile(): Boolean {
        val imageTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
        return mimeType in imageTypes
    }

    fun isPDFFile(): Boolean {
        return mimeType == "application/pdf"
    }

    fun isDocumentFile(): Boolean {
        val docTypes = setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
        )
        return mimeType in docTypes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Document) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Document(id=$id, type=$documentType, name='$documentName', size=$fileSize)"
    }
}

enum class DocumentType {
    CONSTITUTION,
    ANNUAL_RETURN,
    DIRECTOR_CONSENT,
    SHARE_ALLOTMENT,
    SPECIAL_RESOLUTION,
    AMALGAMATION_PROPOSAL,
    LIQUIDATION_NOTICE,
    RECEIVER_NOTICE,
    CERTIFICATE_OF_INCORPORATION,
    CERTIFICATE_OF_COMPLIANCE,
    CHANGE_OF_NAME,
    CHANGE_OF_ADDRESS,
    CHANGE_OF_DIRECTORS,
    CHANGE_OF_SHAREHOLDERS,
    MISCELLANEOUS,
}
