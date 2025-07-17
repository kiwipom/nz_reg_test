package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Address
import nz.govt.companiesoffice.register.entity.AddressType
import nz.govt.companiesoffice.register.entity.Company
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service for managing address change notifications and communications
 */
@Service
@Transactional
class AddressNotificationService(
    private val auditService: AuditService,
) {

    private val logger = LoggerFactory.getLogger(AddressNotificationService::class.java)

    /**
     * Sends notification for address change
     */
    fun sendAddressChangeNotification(
        company: Company,
        addressType: AddressType,
        previousAddress: Address?,
        newAddress: Address,
        notificationType: AddressNotificationType,
        requestedBy: String? = null,
    ): AddressNotificationResult {
        logger.info("Sending address change notification for company ${company.id}, type ${addressType.name}")

        val notification = createAddressNotification(
            company = company,
            addressType = addressType,
            previousAddress = previousAddress,
            newAddress = newAddress,
            notificationType = notificationType,
            requestedBy = requestedBy,
        )

        // Determine notification recipients
        val recipients = determineAddressNotificationRecipients(company, addressType, notificationType)

        // Send notifications to each recipient
        val deliveryResults = recipients.map { recipient ->
            sendNotificationToRecipient(notification, recipient)
        }

        // Log the notification event
        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.CREATE,
            resourceType = "AddressNotification",
            resourceId = "${company.id}-${addressType.name}-${System.currentTimeMillis()}",
            details = mapOf(
                "companyId" to company.id,
                "addressType" to addressType.name,
                "notificationType" to notificationType.name,
                "recipientCount" to recipients.size,
                "successfulDeliveries" to deliveryResults.count { it.isSuccessful },
                "failedDeliveries" to deliveryResults.count { !it.isSuccessful },
                "requestedBy" to requestedBy,
            ),
        )

        return AddressNotificationResult(
            notification = notification,
            deliveryResults = deliveryResults,
            isSuccessful = deliveryResults.all { it.isSuccessful },
            sentAt = LocalDateTime.now(),
        )
    }

    /**
     * Sends bulk address change notifications for multiple address types
     */
    fun sendBulkAddressChangeNotifications(
        company: Company,
        addressChanges: List<AddressChangeNotificationRequest>,
        notificationType: AddressNotificationType = AddressNotificationType.BULK_UPDATE,
        requestedBy: String? = null,
    ): BulkAddressNotificationResult {
        logger.info(
            "Sending bulk address change notifications for company ${company.id} with ${addressChanges.size} changes",
        )

        val notificationResults = addressChanges.map { change ->
            sendAddressChangeNotification(
                company = company,
                addressType = change.addressType,
                previousAddress = change.previousAddress,
                newAddress = change.newAddress,
                notificationType = notificationType,
                requestedBy = requestedBy,
            )
        }

        val summaryNotification = createBulkNotificationSummary(company, addressChanges, notificationResults)
        val summaryDelivery = sendSummaryNotification(summaryNotification)

        auditService.logEvent(
            action = nz.govt.companiesoffice.register.audit.AuditAction.CREATE,
            resourceType = "BulkAddressNotification",
            resourceId = company.id.toString(),
            details = mapOf(
                "companyId" to company.id,
                "addressChangeCount" to addressChanges.size,
                "successfulNotifications" to notificationResults.count { it.isSuccessful },
                "failedNotifications" to notificationResults.count { !it.isSuccessful },
                "requestedBy" to requestedBy,
            ),
        )

        return BulkAddressNotificationResult(
            company = company,
            individualResults = notificationResults,
            summaryResult = summaryDelivery,
            isSuccessful = notificationResults.all { it.isSuccessful } && summaryDelivery.isSuccessful,
            completedAt = LocalDateTime.now(),
        )
    }

    /**
     * Sends reminder notifications for pending address changes
     */
    fun sendAddressChangeReminders(): List<AddressNotificationResult> {
        logger.info("Sending address change reminders")

        // This would typically query for pending address changes from a repository
        // For now, return empty list as placeholder
        val pendingChanges = getPendingAddressChanges()

        return pendingChanges.map { pendingChange ->
            sendAddressChangeNotification(
                company = pendingChange.company,
                addressType = pendingChange.addressType,
                previousAddress = pendingChange.currentAddress,
                newAddress = pendingChange.proposedAddress,
                notificationType = AddressNotificationType.REMINDER,
                requestedBy = "SYSTEM",
            )
        }
    }

    /**
     * Sends compliance notifications for required address updates
     */
    fun sendComplianceNotifications(
        company: Company,
        complianceIssues: List<AddressComplianceIssue>,
    ): List<AddressNotificationResult> {
        logger.info("Sending compliance notifications for company ${company.id}")

        return complianceIssues.map { issue ->
            val notification = createComplianceNotification(company, issue)
            val recipients = determineComplianceRecipients(company, issue)

            val deliveryResults = recipients.map { recipient ->
                sendNotificationToRecipient(notification, recipient)
            }

            AddressNotificationResult(
                notification = notification,
                deliveryResults = deliveryResults,
                isSuccessful = deliveryResults.all { it.isSuccessful },
                sentAt = LocalDateTime.now(),
            )
        }
    }

    /**
     * Validates notification preferences and delivery channels
     */
    fun validateNotificationPreferences(
        company: Company,
        preferences: AddressNotificationPreferences,
    ): NotificationPreferencesValidation {
        logger.debug("Validating notification preferences for company ${company.id}")

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validate email addresses
        preferences.emailAddresses.forEach { email ->
            if (!isValidEmail(email)) {
                errors.add("Invalid email address: $email")
            }
        }

        // Validate phone numbers
        preferences.phoneNumbers.forEach { phone ->
            if (!isValidPhoneNumber(phone)) {
                errors.add("Invalid phone number: $phone")
            }
        }

        // Check for at least one delivery method
        if (preferences.emailAddresses.isEmpty() &&
            preferences.phoneNumbers.isEmpty() &&
            preferences.postalAddress == null
        ) {
            errors.add("At least one notification delivery method must be specified")
        }

        // Validate delivery preferences
        if (preferences.deliveryMethods.isEmpty()) {
            warnings.add("No delivery methods specified, using default email notification")
        }

        return NotificationPreferencesValidation(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            checkedAt = LocalDateTime.now(),
        )
    }

    private fun createAddressNotification(
        company: Company,
        addressType: AddressType,
        previousAddress: Address?,
        newAddress: Address,
        notificationType: AddressNotificationType,
        requestedBy: String?,
    ): AddressNotification {
        val subject = generateNotificationSubject(company, addressType, notificationType)
        val content = generateNotificationContent(
            company = company,
            addressType = addressType,
            previousAddress = previousAddress,
            newAddress = newAddress,
            notificationType = notificationType,
        )

        return AddressNotification(
            id = generateNotificationId(),
            company = company,
            addressType = addressType,
            notificationType = notificationType,
            subject = subject,
            content = content,
            previousAddress = previousAddress,
            newAddress = newAddress,
            requestedBy = requestedBy,
            createdAt = LocalDateTime.now(),
        )
    }

    private fun determineAddressNotificationRecipients(
        company: Company,
        addressType: AddressType,
        notificationType: AddressNotificationType,
    ): List<AddressNotificationRecipient> {
        val recipients = mutableListOf<AddressNotificationRecipient>()

        // Add company contact person - for now use a placeholder
        // In real implementation, this would fetch current addresses from AddressService
        recipients.add(
            AddressNotificationRecipient(
                type = AddressRecipientType.COMPANY_CONTACT,
                deliveryMethod = AddressDeliveryMethod.EMAIL,
                address = "contact@${company.companyName.lowercase().replace(" ", "")}.com",
                name = "Company Contact",
            ),
        )

        // Add directors (placeholder - would fetch from director service)
        // recipients.addAll(getDirectorRecipients(company))

        // Add registrar for certain notification types
        if (notificationType in listOf(
                AddressNotificationType.COMPLIANCE_REQUIRED,
                AddressNotificationType.WORKFLOW_APPROVED,
            )
        ) {
            recipients.add(
                AddressNotificationRecipient(
                    type = AddressRecipientType.REGISTRAR,
                    deliveryMethod = AddressDeliveryMethod.EMAIL,
                    address = "registrar@companiesoffice.govt.nz",
                    name = "Companies Office Registrar",
                ),
            )
        }

        return recipients
    }

    private fun sendNotificationToRecipient(
        notification: AddressNotification,
        recipient: AddressNotificationRecipient,
    ): AddressDeliveryResult {
        logger.debug("Sending notification to ${recipient.type.name}: ${recipient.address}")

        return try {
            when (recipient.deliveryMethod) {
                AddressDeliveryMethod.EMAIL -> sendEmailNotification(notification, recipient)
                AddressDeliveryMethod.SMS -> sendSmsNotification(notification, recipient)
                AddressDeliveryMethod.POST -> sendPostalNotification(notification, recipient)
                AddressDeliveryMethod.PUSH -> sendPushNotification(notification, recipient)
            }
        } catch (e: Exception) {
            logger.error("Failed to send notification to ${recipient.address}: ${e.message}", e)
            AddressDeliveryResult(
                recipient = recipient,
                isSuccessful = false,
                error = e.message,
                attemptedAt = LocalDateTime.now(),
            )
        }
    }

    private fun sendEmailNotification(
        notification: AddressNotification,
        recipient: AddressNotificationRecipient,
    ): AddressDeliveryResult {
        // Placeholder for email service integration
        logger.info("Sending email notification to ${recipient.address}")

        // In real implementation, this would integrate with email service (SES, SendGrid, etc.)
        return AddressDeliveryResult(
            recipient = recipient,
            isSuccessful = true,
            deliveredAt = LocalDateTime.now(),
            attemptedAt = LocalDateTime.now(),
        )
    }

    private fun sendSmsNotification(
        notification: AddressNotification,
        recipient: AddressNotificationRecipient,
    ): AddressDeliveryResult {
        // Placeholder for SMS service integration
        logger.info("Sending SMS notification to ${recipient.address}")

        // In real implementation, this would integrate with SMS service (SNS, Twilio, etc.)
        return AddressDeliveryResult(
            recipient = recipient,
            isSuccessful = true,
            deliveredAt = LocalDateTime.now(),
            attemptedAt = LocalDateTime.now(),
        )
    }

    private fun sendPostalNotification(
        notification: AddressNotification,
        recipient: AddressNotificationRecipient,
    ): AddressDeliveryResult {
        // Placeholder for postal service integration
        logger.info("Sending postal notification to ${recipient.address}")

        // In real implementation, this would integrate with postal service
        return AddressDeliveryResult(
            recipient = recipient,
            isSuccessful = true,
            deliveredAt = LocalDateTime.now(),
            attemptedAt = LocalDateTime.now(),
        )
    }

    private fun sendPushNotification(
        notification: AddressNotification,
        recipient: AddressNotificationRecipient,
    ): AddressDeliveryResult {
        // Placeholder for push notification service integration
        logger.info("Sending push notification to ${recipient.address}")

        // In real implementation, this would integrate with push notification service
        return AddressDeliveryResult(
            recipient = recipient,
            isSuccessful = true,
            deliveredAt = LocalDateTime.now(),
            attemptedAt = LocalDateTime.now(),
        )
    }

    private fun createBulkNotificationSummary(
        company: Company,
        addressChanges: List<AddressChangeNotificationRequest>,
        notificationResults: List<AddressNotificationResult>,
    ): AddressNotification {
        val subject = "Bulk Address Update Summary - ${company.companyName}"
        val content = """
            Dear Company Contact,
            
            This is a summary of the bulk address updates for ${company.companyName} (${company.companyNumber}).
            
            Changes processed: ${addressChanges.size}
            Successful notifications: ${notificationResults.count { it.isSuccessful }}
            Failed notifications: ${notificationResults.count { !it.isSuccessful }}
            
            Address changes:
            ${addressChanges.joinToString("\n") { "- ${it.addressType.name}: ${it.newAddress.getFullAddress()}" }}
            
            If you have any questions, please contact the Companies Office.
            
            Regards,
            Companies Office
        """.trimIndent()

        return AddressNotification(
            id = generateNotificationId(),
            company = company,
            addressType = AddressType.COMMUNICATION, // Use communication for summary
            notificationType = AddressNotificationType.BULK_UPDATE,
            subject = subject,
            content = content,
            previousAddress = null,
            newAddress = null,
            requestedBy = "SYSTEM",
            createdAt = LocalDateTime.now(),
        )
    }

    private fun sendSummaryNotification(notification: AddressNotification): AddressDeliveryResult {
        // Send summary notification to company
        val recipient = AddressNotificationRecipient(
            type = AddressRecipientType.COMPANY_CONTACT,
            deliveryMethod = AddressDeliveryMethod.EMAIL,
            address = "company@example.com", // Would get from company data
            name = "Company Contact",
        )

        return sendNotificationToRecipient(notification, recipient)
    }

    private fun getPendingAddressChanges(): List<PendingAddressChange> {
        // Placeholder for repository query
        return emptyList()
    }

    private fun createComplianceNotification(
        company: Company,
        issue: AddressComplianceIssue,
    ): AddressNotification {
        val subject = "Address Compliance Notice - ${company.companyName}"
        val content = """
            Dear Company Contact,
            
            This is a compliance notice regarding the addresses for ${company.companyName} (${company.companyNumber}).
            
            Issue: ${issue.description}
            Address Type: ${issue.addressType.name}
            Severity: ${issue.severity.name}
            Due Date: ${issue.dueDate}
            
            Please take appropriate action to resolve this issue.
            
            Regards,
            Companies Office Registrar
        """.trimIndent()

        return AddressNotification(
            id = generateNotificationId(),
            company = company,
            addressType = issue.addressType,
            notificationType = AddressNotificationType.COMPLIANCE_REQUIRED,
            subject = subject,
            content = content,
            previousAddress = null,
            newAddress = null,
            requestedBy = "REGISTRAR",
            createdAt = LocalDateTime.now(),
        )
    }

    private fun determineComplianceRecipients(
        company: Company,
        issue: AddressComplianceIssue,
    ): List<AddressNotificationRecipient> {
        val recipients = mutableListOf<AddressNotificationRecipient>()

        // Add company contacts
        recipients.addAll(
            determineAddressNotificationRecipients(
                company,
                issue.addressType,
                AddressNotificationType.COMPLIANCE_REQUIRED,
            ),
        )

        // Add internal recipients for high severity issues
        if (issue.severity == AddressComplianceSeverity.HIGH) {
            recipients.add(
                AddressNotificationRecipient(
                    type = AddressRecipientType.INTERNAL_COMPLIANCE,
                    deliveryMethod = AddressDeliveryMethod.EMAIL,
                    address = "compliance@companiesoffice.govt.nz",
                    name = "Compliance Team",
                ),
            )
        }

        return recipients
    }

    private fun generateNotificationSubject(
        company: Company,
        addressType: AddressType,
        notificationType: AddressNotificationType,
    ): String {
        return when (notificationType) {
            AddressNotificationType.ADDRESS_CHANGED -> "Address Update Confirmation - ${company.companyName}"
            AddressNotificationType.WORKFLOW_APPROVED -> "Address Change Approved - ${company.companyName}"
            AddressNotificationType.WORKFLOW_REJECTED -> "Address Change Rejected - ${company.companyName}"
            AddressNotificationType.COMPLIANCE_REQUIRED -> "Address Compliance Required - ${company.companyName}"
            AddressNotificationType.REMINDER -> "Address Change Reminder - ${company.companyName}"
            AddressNotificationType.BULK_UPDATE -> "Bulk Address Update - ${company.companyName}"
        }
    }

    private fun generateNotificationContent(
        company: Company,
        addressType: AddressType,
        previousAddress: Address?,
        newAddress: Address,
        notificationType: AddressNotificationType,
    ): String {
        return when (notificationType) {
            AddressNotificationType.ADDRESS_CHANGED -> {
                """
                Dear Company Contact,
                
                This confirms that the ${addressType.name.lowercase()} address for ${company.companyName} (${company.companyNumber}) has been updated.
                
                ${if (previousAddress != null) "Previous address: ${previousAddress.getFullAddress()}" else ""}
                New address: ${newAddress.getFullAddress()}
                Effective date: ${newAddress.effectiveFrom}
                
                This change has been recorded in the Companies Register.
                
                Regards,
                Companies Office
                """.trimIndent()
            }
            AddressNotificationType.WORKFLOW_APPROVED -> {
                """
                Dear Company Contact,
                
                Your address change request for ${company.companyName} (${company.companyNumber}) has been approved.
                
                Address type: ${addressType.name}
                New address: ${newAddress.getFullAddress()}
                Effective date: ${newAddress.effectiveFrom}
                
                The change will be processed and updated in the Companies Register.
                
                Regards,
                Companies Office
                """.trimIndent()
            }
            else -> generateGenericNotificationContent(company, addressType, newAddress, notificationType)
        }
    }

    private fun generateGenericNotificationContent(
        company: Company,
        addressType: AddressType,
        newAddress: Address,
        notificationType: AddressNotificationType,
    ): String {
        return """
            Dear Company Contact,
            
            This is a notification regarding the ${addressType.name.lowercase()} address for ${company.companyName} (${company.companyNumber}).
            
            Notification type: ${notificationType.name.replace("_", " ").lowercase()}
            Address: ${newAddress.getFullAddress()}
            
            Please contact the Companies Office if you have any questions.
            
            Regards,
            Companies Office
        """.trimIndent()
    }

    private fun generateNotificationId(): String {
        return "ADDR_NOTIF_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Allow international format with optional spaces, brackets, hyphens
        // Must be at least 8 digits and start with + or digit
        val cleanPhone = phone.replace(Regex("[\\s\\-()]"), "")
        return cleanPhone.matches(Regex("^\\+?[1-9]\\d{7,14}$"))
    }
}

// Data classes for address notifications

data class AddressNotification(
    val id: String,
    val company: Company,
    val addressType: AddressType,
    val notificationType: AddressNotificationType,
    val subject: String,
    val content: String,
    val previousAddress: Address?,
    val newAddress: Address?,
    val requestedBy: String?,
    val createdAt: LocalDateTime,
) {
    fun getNotificationSummary(): String {
        return "Address notification for ${company.companyName}: ${notificationType.name} - ${addressType.name}"
    }
}

enum class AddressNotificationType {
    ADDRESS_CHANGED,
    WORKFLOW_APPROVED,
    WORKFLOW_REJECTED,
    COMPLIANCE_REQUIRED,
    REMINDER,
    BULK_UPDATE,
}

enum class AddressRecipientType {
    COMPANY_CONTACT,
    DIRECTOR,
    SHAREHOLDER,
    REGISTRAR,
    INTERNAL_COMPLIANCE,
    EXTERNAL_STAKEHOLDER,
}

enum class AddressDeliveryMethod {
    EMAIL,
    SMS,
    POST,
    PUSH,
}

enum class AddressComplianceSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

data class AddressNotificationRecipient(
    val type: AddressRecipientType,
    val deliveryMethod: AddressDeliveryMethod,
    val address: String,
    val name: String,
) {
    fun getRecipientDescription(): String {
        return "$name (${type.name}) via ${deliveryMethod.name}: $address"
    }
}

data class AddressDeliveryResult(
    val recipient: AddressNotificationRecipient,
    val isSuccessful: Boolean,
    val deliveredAt: LocalDateTime? = null,
    val attemptedAt: LocalDateTime,
    val error: String? = null,
    val retryCount: Int = 0,
) {
    fun getDeliveryStatus(): String {
        return if (isSuccessful) {
            "Delivered to ${recipient.address} at $deliveredAt"
        } else {
            "Failed to deliver to ${recipient.address}: ${error ?: "Unknown error"}"
        }
    }
}

data class AddressNotificationResult(
    val notification: AddressNotification,
    val deliveryResults: List<AddressDeliveryResult>,
    val isSuccessful: Boolean,
    val sentAt: LocalDateTime,
) {
    fun getSuccessfulDeliveries(): List<AddressDeliveryResult> = deliveryResults.filter { it.isSuccessful }
    fun getFailedDeliveries(): List<AddressDeliveryResult> = deliveryResults.filter { !it.isSuccessful }
    fun getDeliveryRate(): Double = if (deliveryResults.isEmpty()) {
        0.0
    } else {
        deliveryResults.count {
            it.isSuccessful
        }.toDouble() / deliveryResults.size.toDouble()
    }
}

data class BulkAddressNotificationResult(
    val company: Company,
    val individualResults: List<AddressNotificationResult>,
    val summaryResult: AddressDeliveryResult,
    val isSuccessful: Boolean,
    val completedAt: LocalDateTime,
) {
    fun getTotalNotificationsSent(): Int = individualResults.size
    fun getSuccessfulNotifications(): Int = individualResults.count { it.isSuccessful }
    fun getFailedNotifications(): Int = individualResults.count { !it.isSuccessful }
    fun getOverallSuccessRate(): Double = if (individualResults.isEmpty()) {
        0.0
    } else {
        getSuccessfulNotifications().toDouble() / getTotalNotificationsSent().toDouble()
    }
}

data class AddressChangeNotificationRequest(
    val addressType: AddressType,
    val previousAddress: Address?,
    val newAddress: Address,
) {
    fun getChangeDescription(): String {
        return "${addressType.name}: ${previousAddress?.getFullAddress() ?: "New"} → ${newAddress.getFullAddress()}"
    }
}

data class PendingAddressChange(
    val company: Company,
    val addressType: AddressType,
    val currentAddress: Address?,
    val proposedAddress: Address,
    val pendingSince: LocalDateTime,
) {
    fun getDaysPending(): Long {
        return java.time.Duration.between(pendingSince, LocalDateTime.now()).toDays()
    }
}

data class AddressComplianceIssue(
    val addressType: AddressType,
    val description: String,
    val severity: AddressComplianceSeverity,
    val dueDate: LocalDate,
    val identifiedAt: LocalDateTime,
) {
    fun getDaysUntilDue(): Long {
        return java.time.Period.between(LocalDate.now(), dueDate).days.toLong()
    }

    fun isOverdue(): Boolean = LocalDate.now().isAfter(dueDate)
}

data class AddressNotificationPreferences(
    val emailAddresses: List<String>,
    val phoneNumbers: List<String>,
    val postalAddress: Address?,
    val deliveryMethods: List<AddressDeliveryMethod>,
    val notificationTypes: List<AddressNotificationType>,
    val quietHours: Pair<Int, Int>? = null, // Hour range for quiet time
) {
    fun hasEmailNotifications(): Boolean = emailAddresses.isNotEmpty() && AddressDeliveryMethod.EMAIL in deliveryMethods
    fun hasSmsNotifications(): Boolean = phoneNumbers.isNotEmpty() && AddressDeliveryMethod.SMS in deliveryMethods
    fun hasPostalNotifications(): Boolean = postalAddress != null && AddressDeliveryMethod.POST in deliveryMethods
}

data class NotificationPreferencesValidation(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val checkedAt: LocalDateTime,
) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    fun getIssueCount(): Int = errors.size + warnings.size
}
