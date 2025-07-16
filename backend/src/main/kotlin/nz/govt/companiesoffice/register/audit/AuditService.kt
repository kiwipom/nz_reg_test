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
}
