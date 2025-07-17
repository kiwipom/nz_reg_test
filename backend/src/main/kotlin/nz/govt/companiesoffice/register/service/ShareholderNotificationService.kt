package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.entity.ShareAllocation
import nz.govt.companiesoffice.register.entity.Shareholder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service for managing shareholder-related notifications and updates.
 *
 * Handles notifications for:
 * - Shareholder registrations
 * - Shareholder information updates
 * - Share allocations and transfers
 * - Share payment updates
 * - Share cancellations
 * - Compliance reminders
 */
@Service
class ShareholderNotificationService {
    private val logger = LoggerFactory.getLogger(ShareholderNotificationService::class.java)

    /**
     * Sends notification when a new shareholder is registered.
     */
    fun notifyShareholderRegistration(shareholder: Shareholder) {
        logger.info("Sending shareholder registration notifications for: ${shareholder.fullName}")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = ShareholderNotificationType.SHAREHOLDER_REGISTRATION,
                    recipient = ShareholderNotificationRecipient.COMPANY_REGISTRAR,
                    title = "New Shareholder Registered",
                    message = "Shareholder ${shareholder.fullName} has been registered for " +
                        "${shareholder.company.companyName}",
                    shareholder = shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = ShareholderNotificationType.SHAREHOLDER_REGISTRATION,
                    recipient = ShareholderNotificationRecipient.COMPANY_CONTACTS,
                    title = "Shareholder Registration Confirmation",
                    message = "New shareholder ${shareholder.fullName} has been successfully registered " +
                        "with your company",
                    shareholder = shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )

            // Welcome notification to the shareholder
            add(
                createNotification(
                    type = ShareholderNotificationType.SHAREHOLDER_REGISTRATION,
                    recipient = ShareholderNotificationRecipient.SHAREHOLDER_PERSONAL,
                    title = "Welcome as a Shareholder",
                    message = "Welcome! You have been registered as a shareholder of " +
                        "${shareholder.company.companyName}",
                    shareholder = shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when shareholder information is updated.
     */
    fun notifyShareholderUpdate(
        shareholder: Shareholder,
        updatedFields: List<String>,
        isSignificantChange: Boolean = false,
    ) {
        logger.info("Sending shareholder update notifications for: ${shareholder.fullName}")

        val notifications = buildList {
            // Always notify registrar for record keeping
            add(
                createNotification(
                    type = ShareholderNotificationType.SHAREHOLDER_UPDATE,
                    recipient = ShareholderNotificationRecipient.COMPANY_REGISTRAR,
                    title = "Shareholder Information Updated",
                    message = "Shareholder ${shareholder.fullName} information updated. Fields changed: " +
                        updatedFields.joinToString(", "),
                    shareholder = shareholder,
                    priority = ShareholderNotificationPriority.LOW,
                ),
            )

            // Notify company for significant changes only
            if (isSignificantChange) {
                add(
                    createNotification(
                        type = ShareholderNotificationType.SHAREHOLDER_UPDATE,
                        recipient = ShareholderNotificationRecipient.COMPANY_CONTACTS,
                        title = "Shareholder Information Updated",
                        message = "Important shareholder information has been updated for " +
                            "${shareholder.fullName}",
                        shareholder = shareholder,
                        priority = ShareholderNotificationPriority.NORMAL,
                    ),
                )
            }

            // Notify shareholder for their own updates
            add(
                createNotification(
                    type = ShareholderNotificationType.SHAREHOLDER_UPDATE,
                    recipient = ShareholderNotificationRecipient.SHAREHOLDER_PERSONAL,
                    title = "Your Shareholder Information Updated",
                    message = "Your shareholder information has been updated. " +
                        "Please contact us if you did not request this change.",
                    shareholder = shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when shares are allocated to a shareholder.
     */
    fun notifyShareAllocation(shareAllocation: ShareAllocation) {
        logger.info("Sending share allocation notifications for: ${shareAllocation.shareholder.fullName}")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = ShareholderNotificationType.SHARE_ALLOCATION,
                    recipient = ShareholderNotificationRecipient.COMPANY_REGISTRAR,
                    title = "Shares Allocated",
                    message = "${shareAllocation.numberOfShares} ${shareAllocation.shareClass} shares " +
                        "allocated to ${shareAllocation.shareholder.fullName} in " +
                        "${shareAllocation.company.companyName}",
                    shareholder = shareAllocation.shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = ShareholderNotificationType.SHARE_ALLOCATION,
                    recipient = ShareholderNotificationRecipient.COMPANY_CONTACTS,
                    title = "Share Allocation Completed",
                    message = "Share allocation completed: ${shareAllocation.numberOfShares} shares " +
                        "to ${shareAllocation.shareholder.fullName}",
                    shareholder = shareAllocation.shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )

            // Notify shareholder
            add(
                createNotification(
                    type = ShareholderNotificationType.SHARE_ALLOCATION,
                    recipient = ShareholderNotificationRecipient.SHAREHOLDER_PERSONAL,
                    title = "Share Allocation Confirmation",
                    message = "You have been allocated ${shareAllocation.numberOfShares} " +
                        "${shareAllocation.shareClass} shares in ${shareAllocation.company.companyName}. " +
                        "Certificate number: ${shareAllocation.certificateNumber ?: "To be issued"}",
                    shareholder = shareAllocation.shareholder,
                    priority = ShareholderNotificationPriority.HIGH,
                ),
            )
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when shares are transferred.
     */
    fun notifyShareTransfer(
        oldAllocation: ShareAllocation,
        newAllocation: ShareAllocation,
        transferDate: LocalDate,
    ) {
        logger.info("Sending share transfer notifications")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = ShareholderNotificationType.SHARE_TRANSFER,
                    recipient = ShareholderNotificationRecipient.COMPANY_REGISTRAR,
                    title = "Share Transfer Completed",
                    message = "${oldAllocation.numberOfShares} ${oldAllocation.shareClass} shares " +
                        "transferred from ${oldAllocation.shareholder.fullName} " +
                        "to ${newAllocation.shareholder.fullName} on $transferDate",
                    shareholder = newAllocation.shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = ShareholderNotificationType.SHARE_TRANSFER,
                    recipient = ShareholderNotificationRecipient.COMPANY_CONTACTS,
                    title = "Share Transfer Notification",
                    message = "Share transfer completed between shareholders on $transferDate",
                    shareholder = newAllocation.shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )

            // Notify transferring shareholder (seller)
            add(
                createNotification(
                    type = ShareholderNotificationType.SHARE_TRANSFER,
                    recipient = ShareholderNotificationRecipient.SHAREHOLDER_PERSONAL,
                    title = "Share Transfer Confirmation - Shares Transferred",
                    message = "Your ${oldAllocation.numberOfShares} ${oldAllocation.shareClass} shares " +
                        "in ${oldAllocation.company.companyName} have been transferred successfully on $transferDate",
                    shareholder = oldAllocation.shareholder,
                    priority = ShareholderNotificationPriority.HIGH,
                ),
            )

            // Notify receiving shareholder (buyer)
            add(
                createNotification(
                    type = ShareholderNotificationType.SHARE_TRANSFER,
                    recipient = ShareholderNotificationRecipient.SHAREHOLDER_PERSONAL,
                    title = "Share Transfer Confirmation - Shares Received",
                    message = "You have received ${newAllocation.numberOfShares} ${newAllocation.shareClass} shares " +
                        "in ${newAllocation.company.companyName} on $transferDate. " +
                        "Certificate number: ${newAllocation.certificateNumber ?: "To be issued"}",
                    shareholder = newAllocation.shareholder,
                    priority = ShareholderNotificationPriority.HIGH,
                ),
            )
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when payment is made on shares.
     */
    fun notifyPaymentUpdate(shareAllocation: ShareAllocation, additionalPayment: BigDecimal) {
        logger.info("Sending payment update notifications for: ${shareAllocation.shareholder.fullName}")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = ShareholderNotificationType.PAYMENT_UPDATE,
                    recipient = ShareholderNotificationRecipient.COMPANY_REGISTRAR,
                    title = "Share Payment Received",
                    message = "Payment of $$additionalPayment received from ${shareAllocation.shareholder.fullName} " +
                        "for ${shareAllocation.shareClass} shares",
                    shareholder = shareAllocation.shareholder,
                    priority = ShareholderNotificationPriority.LOW,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = ShareholderNotificationType.PAYMENT_UPDATE,
                    recipient = ShareholderNotificationRecipient.COMPANY_CONTACTS,
                    title = "Share Payment Received",
                    message = "Share payment of $$additionalPayment received from " +
                        "${shareAllocation.shareholder.fullName}",
                    shareholder = shareAllocation.shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )

            // Notify shareholder
            add(
                createNotification(
                    type = ShareholderNotificationType.PAYMENT_UPDATE,
                    recipient = ShareholderNotificationRecipient.SHAREHOLDER_PERSONAL,
                    title = "Share Payment Confirmation",
                    message = "Your payment of $$additionalPayment for ${shareAllocation.shareClass} shares " +
                        "has been received and processed. Total paid: $${shareAllocation.amountPaid}",
                    shareholder = shareAllocation.shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when share allocation is cancelled.
     */
    fun notifyAllocationCancellation(shareAllocation: ShareAllocation, reason: String) {
        logger.info("Sending allocation cancellation notifications for: ${shareAllocation.shareholder.fullName}")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = ShareholderNotificationType.ALLOCATION_CANCELLATION,
                    recipient = ShareholderNotificationRecipient.COMPANY_REGISTRAR,
                    title = "Share Allocation Cancelled",
                    message = "Share allocation cancelled for ${shareAllocation.shareholder.fullName}. " +
                        "Reason: $reason",
                    shareholder = shareAllocation.shareholder,
                    priority = ShareholderNotificationPriority.HIGH,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = ShareholderNotificationType.ALLOCATION_CANCELLATION,
                    recipient = ShareholderNotificationRecipient.COMPANY_CONTACTS,
                    title = "Share Allocation Cancelled",
                    message = "Share allocation for ${shareAllocation.shareholder.fullName} has been cancelled",
                    shareholder = shareAllocation.shareholder,
                    priority = ShareholderNotificationPriority.HIGH,
                ),
            )

            // Notify shareholder
            add(
                createNotification(
                    type = ShareholderNotificationType.ALLOCATION_CANCELLATION,
                    recipient = ShareholderNotificationRecipient.SHAREHOLDER_PERSONAL,
                    title = "Share Allocation Cancelled",
                    message = "Your share allocation in ${shareAllocation.company.companyName} has been cancelled. " +
                        "Please contact us for more information.",
                    shareholder = shareAllocation.shareholder,
                    priority = ShareholderNotificationPriority.URGENT,
                ),
            )
        }

        sendNotifications(notifications)
    }

    /**
     * Sends notification when a shareholder is removed.
     */
    fun notifyShareholderRemoval(shareholder: Shareholder, reason: String) {
        logger.info("Sending shareholder removal notifications for: ${shareholder.fullName}")

        val notifications = buildList {
            // Notify company registrar
            add(
                createNotification(
                    type = ShareholderNotificationType.SHAREHOLDER_REMOVAL,
                    recipient = ShareholderNotificationRecipient.COMPANY_REGISTRAR,
                    title = "Shareholder Removed",
                    message = "Shareholder ${shareholder.fullName} has been removed from " +
                        "${shareholder.company.companyName}. Reason: $reason",
                    shareholder = shareholder,
                    priority = ShareholderNotificationPriority.HIGH,
                ),
            )

            // Notify company contacts
            add(
                createNotification(
                    type = ShareholderNotificationType.SHAREHOLDER_REMOVAL,
                    recipient = ShareholderNotificationRecipient.COMPANY_CONTACTS,
                    title = "Shareholder Removal Notice",
                    message = "Shareholder ${shareholder.fullName} has been removed from your company",
                    shareholder = shareholder,
                    priority = ShareholderNotificationPriority.NORMAL,
                ),
            )

            // Notify the removed shareholder
            add(
                createNotification(
                    type = ShareholderNotificationType.SHAREHOLDER_REMOVAL,
                    recipient = ShareholderNotificationRecipient.SHAREHOLDER_PERSONAL,
                    title = "Shareholder Status Update",
                    message = "Your shareholder status with ${shareholder.company.companyName} has ended. " +
                        "Please contact us if you have any questions.",
                    shareholder = shareholder,
                    priority = ShareholderNotificationPriority.HIGH,
                ),
            )
        }

        sendNotifications(notifications)
    }

    private fun createNotification(
        type: ShareholderNotificationType,
        recipient: ShareholderNotificationRecipient,
        title: String,
        message: String,
        shareholder: Shareholder,
        priority: ShareholderNotificationPriority = ShareholderNotificationPriority.NORMAL,
    ): ShareholderNotification {
        return ShareholderNotification(
            id = generateNotificationId(),
            type = type,
            recipient = recipient,
            title = title,
            message = message,
            shareholderId = shareholder.id,
            shareholderName = shareholder.fullName,
            companyId = shareholder.company.id,
            companyName = shareholder.company.companyName,
            priority = priority,
            status = ShareholderNotificationStatus.PENDING,
            createdAt = LocalDateTime.now(),
        )
    }

    private fun sendNotifications(notifications: List<ShareholderNotification>) {
        notifications.forEach { notification ->
            try {
                when (notification.recipient) {
                    ShareholderNotificationRecipient.COMPANY_REGISTRAR -> sendToRegistrar(notification)
                    ShareholderNotificationRecipient.COMPANY_CONTACTS -> sendToCompanyContacts(notification)
                    ShareholderNotificationRecipient.SHAREHOLDER_PERSONAL -> sendToShareholder(notification)
                    ShareholderNotificationRecipient.EXTERNAL_AGENCIES -> sendToExternalAgencies(notification)
                }

                logger.info("Notification sent successfully: ${notification.id}")
            } catch (e: Exception) {
                logger.error("Failed to send notification: ${notification.id}", e)
                // In a real implementation, this would be stored for retry
            }
        }
    }

    private fun sendToRegistrar(notification: ShareholderNotification) {
        // Implementation would integrate with registrar notification system
        logger.info("Sending notification to registrar: ${notification.title}")
        // Could integrate with email service, internal messaging system, etc.
    }

    private fun sendToCompanyContacts(notification: ShareholderNotification) {
        // Implementation would look up company contact details and send notifications
        logger.info("Sending notification to company contacts: ${notification.title}")
        // Could send emails, SMS, postal notifications, etc.
    }

    private fun sendToShareholder(notification: ShareholderNotification) {
        // Implementation would send notifications to shareholder's personal contact details
        logger.info("Sending notification to shareholder: ${notification.title}")
        // Would respect shareholder's communication preferences
    }

    private fun sendToExternalAgencies(notification: ShareholderNotification) {
        // Implementation would notify external agencies (tax office, etc.)
        logger.info("Sending notification to external agencies: ${notification.title}")
        // Could integrate with government systems, APIs, etc.
    }

    private fun generateNotificationId(): String {
        return "SN-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
}

/**
 * Types of shareholder notifications.
 */
enum class ShareholderNotificationType {
    SHAREHOLDER_REGISTRATION,
    SHAREHOLDER_UPDATE,
    SHAREHOLDER_REMOVAL,
    SHARE_ALLOCATION,
    SHARE_TRANSFER,
    PAYMENT_UPDATE,
    ALLOCATION_CANCELLATION,
}

/**
 * Shareholder notification recipients.
 */
enum class ShareholderNotificationRecipient {
    COMPANY_REGISTRAR, // Companies Office staff
    COMPANY_CONTACTS, // Company contact persons
    SHAREHOLDER_PERSONAL, // Shareholder's personal contacts
    EXTERNAL_AGENCIES, // External government agencies
}

/**
 * Shareholder notification priority levels.
 */
enum class ShareholderNotificationPriority {
    LOW, // General information
    NORMAL, // Standard business updates
    HIGH, // Important changes requiring attention
    URGENT, // Immediate attention required
    CRITICAL, // Critical issues requiring immediate action
}

/**
 * Shareholder notification status.
 */
enum class ShareholderNotificationStatus {
    PENDING, // Not yet sent
    SENT, // Successfully sent
    FAILED, // Failed to send
    RETRY, // Scheduled for retry
}

/**
 * Shareholder notification data class.
 */
data class ShareholderNotification(
    val id: String,
    val type: ShareholderNotificationType,
    val recipient: ShareholderNotificationRecipient,
    val title: String,
    val message: String,
    val shareholderId: Long?,
    val shareholderName: String?,
    val companyId: Long?,
    val companyName: String?,
    val priority: ShareholderNotificationPriority,
    val status: ShareholderNotificationStatus,
    val createdAt: LocalDateTime,
    val sentAt: LocalDateTime? = null,
    val failureReason: String? = null,
)
