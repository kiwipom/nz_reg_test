package nz.govt.companiesoffice.register.audit

import java.time.LocalDateTime

data class AuditEvent(
    val id: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val userId: String?,
    val userEmail: String?,
    val action: AuditAction,
    val resourceType: String,
    val resourceId: String?,
    val details: Map<String, Any?> = emptyMap(),
    val ipAddress: String?,
    val userAgent: String?,
    val success: Boolean = true,
    val errorMessage: String? = null,
)

enum class AuditAction(val description: String) {
    CREATE("Resource created"),
    READ("Resource accessed"),
    UPDATE("Resource updated"),
    DELETE("Resource deleted"),
    SEARCH("Resource searched"),
    LOGIN("User logged in"),
    LOGOUT("User logged out"),
    PERMISSION_DENIED("Permission denied"),
    SYSTEM_ERROR("System error occurred"),
}
