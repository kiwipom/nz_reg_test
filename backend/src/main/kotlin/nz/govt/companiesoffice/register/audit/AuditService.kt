package nz.govt.companiesoffice.register.audit

import nz.govt.companiesoffice.register.security.SecurityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

@Service
class AuditService {

    private val logger: Logger = LoggerFactory.getLogger(AuditService::class.java)

    fun logEvent(
        action: AuditAction,
        resourceType: String,
        resourceId: String? = null,
        details: Map<String, Any?> = emptyMap(),
        success: Boolean = true,
        errorMessage: String? = null,
    ) {
        val event = createAuditEvent(
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            details = details,
            success = success,
            errorMessage = errorMessage,
        )

        persistAuditEvent(event)
    }

    private fun createAuditEvent(
        action: AuditAction,
        resourceType: String,
        resourceId: String?,
        details: Map<String, Any?>,
        success: Boolean,
        errorMessage: String?,
    ): AuditEvent {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

        return AuditEvent(
            id = UUID.randomUUID().toString(),
            userId = SecurityUtils.getCurrentUserSubject(),
            userEmail = SecurityUtils.getCurrentUserEmail(),
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            details = details,
            ipAddress = request?.remoteAddr,
            userAgent = request?.getHeader("User-Agent"),
            success = success,
            errorMessage = errorMessage,
        )
    }

    private fun persistAuditEvent(event: AuditEvent) {
        // For now, just log to application logs
        // In production, this should be persisted to a database or audit system
        when (event.success) {
            true -> logger.info(
                "AUDIT: {} - {} {} {} by user {} ({})",
                event.action,
                event.resourceType,
                event.resourceId ?: "N/A",
                event.action.description,
                event.userEmail ?: "anonymous",
                event.userId ?: "unknown",
            )
            false -> logger.warn(
                "AUDIT: FAILED {} - {} {} {} by user {} ({}) - Error: {}",
                event.action,
                event.resourceType,
                event.resourceId ?: "N/A",
                event.action.description,
                event.userEmail ?: "anonymous",
                event.userId ?: "unknown",
                event.errorMessage ?: "Unknown error",
            )
        }

        // TODO: Implement database persistence
        // auditRepository.save(event)
    }

    fun logCompanyAccess(companyId: Long) {
        logEvent(
            action = AuditAction.READ,
            resourceType = "Company",
            resourceId = companyId.toString(),
        )
    }

    fun logCompanyCreation(companyId: Long, companyName: String) {
        logEvent(
            action = AuditAction.CREATE,
            resourceType = "Company",
            resourceId = companyId.toString(),
            details = mapOf("companyName" to companyName),
        )
    }

    fun logCompanyUpdate(companyId: Long, changes: Map<String, Any?>) {
        logEvent(
            action = AuditAction.UPDATE,
            resourceType = "Company",
            resourceId = companyId.toString(),
            details = changes,
        )
    }

    fun logCompanySearch(query: String, resultCount: Int) {
        logEvent(
            action = AuditAction.SEARCH,
            resourceType = "Company",
            details = mapOf(
                "query" to query,
                "resultCount" to resultCount,
            ),
        )
    }

    fun logPermissionDenied(action: String, resourceType: String, resourceId: String? = null) {
        logEvent(
            action = AuditAction.PERMISSION_DENIED,
            resourceType = resourceType,
            resourceId = resourceId,
            details = mapOf("attemptedAction" to action),
            success = false,
            errorMessage = "Insufficient permissions",
        )
    }

    // Director audit methods
    fun logDirectorAppointment(directorId: Long, companyId: Long, directorName: String) {
        logEvent(
            action = AuditAction.CREATE,
            resourceType = "Director",
            resourceId = directorId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "directorName" to directorName,
                "action" to "appointment",
            ),
        )
    }

    fun logDirectorResignation(
        directorId: Long,
        companyId: Long,
        directorName: String,
        resignationDate: java.time.LocalDate,
    ) {
        logEvent(
            action = AuditAction.UPDATE,
            resourceType = "Director",
            resourceId = directorId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "directorName" to directorName,
                "action" to "resignation",
                "resignationDate" to resignationDate.toString(),
            ),
        )
    }

    fun logDirectorUpdate(directorId: Long, companyId: Long, directorName: String) {
        logEvent(
            action = AuditAction.UPDATE,
            resourceType = "Director",
            resourceId = directorId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "directorName" to directorName,
                "action" to "update",
            ),
        )
    }

    fun logDirectorConsent(directorId: Long, companyId: Long, directorName: String, consentDate: java.time.LocalDate) {
        logEvent(
            action = AuditAction.UPDATE,
            resourceType = "Director",
            resourceId = directorId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "directorName" to directorName,
                "action" to "consent",
                "consentDate" to consentDate.toString(),
            ),
        )
    }

    fun logDirectorSearch(searchTerm: String, resultCount: Int) {
        logEvent(
            action = AuditAction.SEARCH,
            resourceType = "Director",
            details = mapOf(
                "searchTerm" to searchTerm,
                "resultCount" to resultCount,
            ),
        )
    }

    fun logDirectorDisqualification(directorId: Long, companyId: Long, directorName: String, reason: String) {
        logEvent(
            action = AuditAction.UPDATE,
            resourceType = "Director",
            resourceId = directorId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "directorName" to directorName,
                "action" to "disqualification",
                "reason" to reason,
            ),
        )
    }

    fun logDirectorDeletion(directorId: Long, companyId: Long, directorName: String) {
        logEvent(
            action = AuditAction.DELETE,
            resourceType = "Director",
            resourceId = directorId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "directorName" to directorName,
                "action" to "deletion",
            ),
        )
    }

    // Shareholder audit methods
    fun logShareholderCreation(shareholderId: Long, companyId: Long, shareholderName: String) {
        logEvent(
            action = AuditAction.CREATE,
            resourceType = "Shareholder",
            resourceId = shareholderId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "shareholderName" to shareholderName,
                "action" to "creation",
            ),
        )
    }

    fun logShareholderUpdate(shareholderId: Long, companyId: Long, shareholderName: String) {
        logEvent(
            action = AuditAction.UPDATE,
            resourceType = "Shareholder",
            resourceId = shareholderId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "shareholderName" to shareholderName,
                "action" to "update",
            ),
        )
    }

    fun logShareholderSearch(searchTerm: String, resultCount: Int) {
        logEvent(
            action = AuditAction.SEARCH,
            resourceType = "Shareholder",
            details = mapOf(
                "searchTerm" to searchTerm,
                "resultCount" to resultCount,
            ),
        )
    }

    fun logShareholderDeletion(shareholderId: Long, companyId: Long, shareholderName: String) {
        logEvent(
            action = AuditAction.DELETE,
            resourceType = "Shareholder",
            resourceId = shareholderId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "shareholderName" to shareholderName,
                "action" to "deletion",
            ),
        )
    }

    // Address audit methods
    fun logAddressChange(
        addressId: Long,
        companyId: Long,
        addressType: String,
        effectiveDate: java.time.LocalDate,
    ) {
        logEvent(
            action = AuditAction.UPDATE,
            resourceType = "Address",
            resourceId = addressId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "addressType" to addressType,
                "effectiveDate" to effectiveDate,
                "action" to "ADDRESS_CHANGED",
            ),
        )
    }

    fun logAddressValidation(companyId: Long, addressType: String, validationResult: String) {
        logEvent(
            action = AuditAction.SEARCH,
            resourceType = "Address",
            details = mapOf(
                "companyId" to companyId,
                "addressType" to addressType,
                "validationResult" to validationResult,
                "action" to "ADDRESS_VALIDATION",
            ),
        )
    }

    // Document audit methods
    fun logDocumentUpload(
        documentId: Long,
        companyId: Long,
        documentType: String,
        fileName: String,
        uploadedBy: String?,
    ) {
        logEvent(
            action = AuditAction.CREATE,
            resourceType = "Document",
            resourceId = documentId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "documentType" to documentType,
                "fileName" to fileName,
                "uploadedBy" to uploadedBy,
                "action" to "DOCUMENT_UPLOADED",
            ),
        )
    }

    fun logDocumentDownload(
        documentId: Long,
        companyId: Long,
        documentType: String,
        fileName: String,
        downloadedBy: String?,
    ) {
        logEvent(
            action = AuditAction.READ,
            resourceType = "Document",
            resourceId = documentId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "documentType" to documentType,
                "fileName" to fileName,
                "downloadedBy" to downloadedBy,
                "action" to "DOCUMENT_DOWNLOADED",
            ),
        )
    }

    fun logDocumentDeletion(
        documentId: Long,
        companyId: Long,
        documentType: String,
        fileName: String,
        deletedBy: String?,
    ) {
        logEvent(
            action = AuditAction.DELETE,
            resourceType = "Document",
            resourceId = documentId.toString(),
            details = mapOf(
                "companyId" to companyId,
                "documentType" to documentType,
                "fileName" to fileName,
                "deletedBy" to deletedBy,
                "action" to "DOCUMENT_DELETED",
            ),
        )
    }
}
