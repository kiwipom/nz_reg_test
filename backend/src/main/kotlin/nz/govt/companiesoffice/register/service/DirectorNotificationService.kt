package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.entity.Director
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service for managing director-related notifications and updates.
 *
 * Handles notifications for:
 * - Director appointments
 * - Director resignations
 * - Director disqualifications
 * - Director information updates
 * - Compliance violations
 * - Residency status changes
 */
@Service
class DirectorNotificationService {
    private val logger = LoggerFactory.getLogger(DirectorNotificationService::class.java)

    /**
     * Sends notification when a director is appointed.
     */
    fun notifyDirectorAppointment(director: Director) {
        logger.info("Sending director appointment notifications for: ${director.fullName}")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = NotificationType.DIRECTOR_APPOINTMENT,
                    recipient = NotificationRecipient.COMPANY_REGISTRAR,
                    title = "New Director Appointed",
                    message = "Director ${director.fullName} has been appointed to ${director.company.companyName}",
                    director = director,
                    priority = NotificationPriority.NORMAL,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = NotificationType.DIRECTOR_APPOINTMENT,
                    recipient = NotificationRecipient.COMPANY_CONTACTS,
                    title = "Director Appointment Confirmation",
                    message = "New director ${director.fullName} has been successfully appointed to your company",
                    director = director,
                    priority = NotificationPriority.NORMAL,
                ),
            )

            // If consent is required, notify about missing consent
            if (!director.consentGiven) {
                add(
                    createNotification(
                        type = NotificationType.CONSENT_REQUIRED,
                        recipient = NotificationRecipient.DIRECTOR_PERSONAL,
                        title = "Director Consent Required",
                        message = "Your consent is required to complete your director appointment for " +
                            director.company.companyName,
                        director = director,
                        priority = NotificationPriority.HIGH,
                    ),
                )
            }
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when a director resigns.
     */
    fun notifyDirectorResignation(director: Director, resignationDate: LocalDate) {
        logger.info("Sending director resignation notifications for: ${director.fullName}")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = NotificationType.DIRECTOR_RESIGNATION,
                    recipient = NotificationRecipient.COMPANY_REGISTRAR,
                    title = "Director Resignation",
                    message = "Director ${director.fullName} has resigned from " +
                        "${director.company.companyName} effective $resignationDate",
                    director = director,
                    priority = NotificationPriority.NORMAL,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = NotificationType.DIRECTOR_RESIGNATION,
                    recipient = NotificationRecipient.COMPANY_CONTACTS,
                    title = "Director Resignation Notice",
                    message = "Director ${director.fullName} has resigned from your company effective $resignationDate",
                    director = director,
                    priority = NotificationPriority.NORMAL,
                ),
            )

            // Check for compliance issues after resignation
            val complianceIssues = checkComplianceAfterResignation(director)
            if (complianceIssues.isNotEmpty()) {
                add(
                    createNotification(
                        type = NotificationType.COMPLIANCE_VIOLATION,
                        recipient = NotificationRecipient.COMPANY_CONTACTS,
                        title = "URGENT: Compliance Issue After Director Resignation",
                        message = "Director resignation has created compliance issues: ${complianceIssues.joinToString(
                            ", ",
                        )}",
                        director = director,
                        priority = NotificationPriority.URGENT,
                    ),
                )
            }
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when a director is disqualified.
     */
    fun notifyDirectorDisqualification(director: Director, reason: String) {
        logger.info("Sending director disqualification notifications for: ${director.fullName}")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = NotificationType.DIRECTOR_DISQUALIFICATION,
                    recipient = NotificationRecipient.COMPANY_REGISTRAR,
                    title = "Director Disqualification",
                    message = "Director ${director.fullName} has been disqualified from " +
                        "${director.company.companyName}. Reason: $reason",
                    director = director,
                    priority = NotificationPriority.HIGH,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = NotificationType.DIRECTOR_DISQUALIFICATION,
                    recipient = NotificationRecipient.COMPANY_CONTACTS,
                    title = "URGENT: Director Disqualification Notice",
                    message = "Director ${director.fullName} has been disqualified. " +
                        "Your company may need to appoint a replacement director immediately.",
                    director = director,
                    priority = NotificationPriority.URGENT,
                ),
            )

            // Check for compliance issues after disqualification
            val complianceIssues = checkComplianceAfterDisqualification(director)
            if (complianceIssues.isNotEmpty()) {
                add(
                    createNotification(
                        type = NotificationType.COMPLIANCE_VIOLATION,
                        recipient = NotificationRecipient.COMPANY_CONTACTS,
                        title = "CRITICAL: Immediate Action Required",
                        message = "Director disqualification has created critical compliance issues: " +
                            complianceIssues.joinToString(", ") + ". Immediate action required.",
                        director = director,
                        priority = NotificationPriority.CRITICAL,
                    ),
                )
            }
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when director information is updated.
     */
    fun notifyDirectorUpdate(
        director: Director,
        updatedFields: List<String>,
        isSignificantChange: Boolean = false,
    ) {
        logger.info("Sending director update notifications for: ${director.fullName}")

        val notifications = buildList {
            // Always notify registrar for record keeping
            add(
                createNotification(
                    type = NotificationType.DIRECTOR_UPDATE,
                    recipient = NotificationRecipient.COMPANY_REGISTRAR,
                    title = "Director Information Updated",
                    message = "Director ${director.fullName} information updated. Fields changed: " +
                        updatedFields.joinToString(", "),
                    director = director,
                    priority = NotificationPriority.LOW,
                ),
            )

            // Notify company for significant changes only
            if (isSignificantChange) {
                add(
                    createNotification(
                        type = NotificationType.DIRECTOR_UPDATE,
                        recipient = NotificationRecipient.COMPANY_CONTACTS,
                        title = "Director Information Updated",
                        message = "Important director information has been updated for ${director.fullName}",
                        director = director,
                        priority = NotificationPriority.NORMAL,
                    ),
                )
            }
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when director consent is given.
     */
    fun notifyDirectorConsent(director: Director, consentDate: LocalDate) {
        logger.info("Sending director consent notifications for: ${director.fullName}")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = NotificationType.CONSENT_RECEIVED,
                    recipient = NotificationRecipient.COMPANY_REGISTRAR,
                    title = "Director Consent Received",
                    message = "Consent received from director ${director.fullName} for " +
                        "${director.company.companyName} on $consentDate",
                    director = director,
                    priority = NotificationPriority.NORMAL,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = NotificationType.CONSENT_RECEIVED,
                    recipient = NotificationRecipient.COMPANY_CONTACTS,
                    title = "Director Consent Completed",
                    message = "Director ${director.fullName} has provided required consent for their appointment",
                    director = director,
                    priority = NotificationPriority.NORMAL,
                ),
            )
        }

        sendNotifications(notifications)
    }

    /**
     * Sends periodic compliance reminders.
     */
    fun sendComplianceReminders(companyId: Long, issues: List<ComplianceIssue>) {
        logger.info("Sending compliance reminders for company: $companyId")

        val notifications = issues.map { issue ->
            createNotification(
                type = NotificationType.COMPLIANCE_REMINDER,
                recipient = NotificationRecipient.COMPANY_CONTACTS,
                title = "Director Compliance Reminder",
                message = issue.description,
                companyId = companyId,
                priority = when (issue.severity) {
                    ComplianceSeverity.LOW -> NotificationPriority.LOW
                    ComplianceSeverity.MEDIUM -> NotificationPriority.NORMAL
                    ComplianceSeverity.HIGH -> NotificationPriority.HIGH
                    ComplianceSeverity.CRITICAL -> NotificationPriority.CRITICAL
                },
            )
        }

        sendNotifications(notifications)
    }

    private fun createNotification(
        type: NotificationType,
        recipient: NotificationRecipient,
        title: String,
        message: String,
        director: Director? = null,
        companyId: Long? = null,
        priority: NotificationPriority = NotificationPriority.NORMAL,
    ): DirectorNotification {
        return DirectorNotification(
            id = generateNotificationId(),
            type = type,
            recipient = recipient,
            title = title,
            message = message,
            directorId = director?.id,
            directorName = director?.fullName,
            companyId = companyId ?: director?.company?.id,
            companyName = director?.company?.companyName,
            priority = priority,
            status = NotificationStatus.PENDING,
            createdAt = LocalDateTime.now(),
        )
    }

    private fun sendNotifications(notifications: List<DirectorNotification>) {
        notifications.forEach { notification ->
            try {
                when (notification.recipient) {
                    NotificationRecipient.COMPANY_REGISTRAR -> sendToRegistrar(notification)
                    NotificationRecipient.COMPANY_CONTACTS -> sendToCompanyContacts(notification)
                    NotificationRecipient.DIRECTOR_PERSONAL -> sendToDirector(notification)
                    NotificationRecipient.EXTERNAL_AGENCIES -> sendToExternalAgencies(notification)
                }

                logger.info("Notification sent successfully: ${notification.id}")
            } catch (e: Exception) {
                logger.error("Failed to send notification: ${notification.id}", e)
                // In a real implementation, this would be stored for retry
            }
        }
    }

    private fun sendToRegistrar(notification: DirectorNotification) {
        // Implementation would integrate with registrar notification system
        logger.info("Sending notification to registrar: ${notification.title}")
        // Could integrate with email service, internal messaging system, etc.
    }

    private fun sendToCompanyContacts(notification: DirectorNotification) {
        // Implementation would look up company contact details and send notifications
        logger.info("Sending notification to company contacts: ${notification.title}")
        // Could send emails, SMS, postal notifications, etc.
    }

    private fun sendToDirector(notification: DirectorNotification) {
        // Implementation would send notifications to director's personal contact details
        logger.info("Sending notification to director: ${notification.title}")
        // Would respect director's communication preferences
    }

    private fun sendToExternalAgencies(notification: DirectorNotification) {
        // Implementation would notify external agencies (tax office, etc.)
        logger.info("Sending notification to external agencies: ${notification.title}")
        // Could integrate with government systems, APIs, etc.
    }

    private fun checkComplianceAfterResignation(director: Director): List<String> {
        // In a real implementation, this would check company compliance
        return emptyList() // Placeholder
    }

    private fun checkComplianceAfterDisqualification(director: Director): List<String> {
        // In a real implementation, this would check company compliance
        return emptyList() // Placeholder
    }

    private fun generateNotificationId(): String {
        return "DN-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
}

/**
 * Types of director notifications.
 */
enum class NotificationType {
    DIRECTOR_APPOINTMENT,
    DIRECTOR_RESIGNATION,
    DIRECTOR_DISQUALIFICATION,
    DIRECTOR_UPDATE,
    CONSENT_REQUIRED,
    CONSENT_RECEIVED,
    COMPLIANCE_VIOLATION,
    COMPLIANCE_REMINDER,
}

/**
 * Notification recipients.
 */
enum class NotificationRecipient {
    COMPANY_REGISTRAR, // Companies Office staff
    COMPANY_CONTACTS, // Company contact persons
    DIRECTOR_PERSONAL, // Director's personal contacts
    EXTERNAL_AGENCIES, // External government agencies
}

/**
 * Notification priority levels.
 */
enum class NotificationPriority {
    LOW, // General information
    NORMAL, // Standard business updates
    HIGH, // Important changes requiring attention
    URGENT, // Immediate attention required
    CRITICAL, // Critical issues requiring immediate action
}

/**
 * Notification status.
 */
enum class NotificationStatus {
    PENDING, // Not yet sent
    SENT, // Successfully sent
    FAILED, // Failed to send
    RETRY, // Scheduled for retry
}

/**
 * Compliance issue severity levels.
 */
enum class ComplianceSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/**
 * Director notification data class.
 */
data class DirectorNotification(
    val id: String,
    val type: NotificationType,
    val recipient: NotificationRecipient,
    val title: String,
    val message: String,
    val directorId: Long?,
    val directorName: String?,
    val companyId: Long?,
    val companyName: String?,
    val priority: NotificationPriority,
    val status: NotificationStatus,
    val createdAt: LocalDateTime,
    val sentAt: LocalDateTime? = null,
    val failureReason: String? = null,
)

/**
 * Compliance issue data class.
 */
data class ComplianceIssue(
    val type: String,
    val description: String,
    val severity: ComplianceSeverity,
    val dueDate: LocalDate? = null,
)
