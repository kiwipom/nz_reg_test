package nz.govt.companiesoffice.register.service

import io.mockk.mockk
import io.mockk.verify
import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Address
import nz.govt.companiesoffice.register.entity.AddressType
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("AddressNotificationService Tests")
class AddressNotificationServiceTest {

    private lateinit var auditService: AuditService
    private lateinit var addressNotificationService: AddressNotificationService
    private lateinit var testCompany: Company
    private lateinit var testAddress: Address

    @BeforeEach
    fun setUp() {
        auditService = mockk(relaxed = true)
        addressNotificationService = AddressNotificationService(auditService)

        testCompany = Company(
            id = 1L,
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.now(),
        )

        testAddress = Address(
            company = testCompany,
            addressType = AddressType.REGISTERED,
            addressLine1 = "123 Test Street",
            city = "Auckland",
            country = "NZ",
            postcode = "1010",
            email = "test@example.com",
            effectiveFrom = LocalDate.now(),
        )
    }

    @Nested
    @DisplayName("Address Change Notifications")
    inner class AddressChangeNotifications {

        @Test
        fun `should send address change notification successfully`() {
            // Given
            val previousAddress = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Test Street",
                city = "Auckland",
                country = "NZ",
                postcode = "1010",
                email = "test@example.com",
                effectiveFrom = LocalDate.now(),
            )
            val newAddress = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "456 New Street",
                city = "Auckland",
                country = "NZ",
                postcode = "1010",
                email = "test@example.com",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressNotificationService.sendAddressChangeNotification(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                previousAddress = previousAddress,
                newAddress = newAddress,
                notificationType = AddressNotificationType.ADDRESS_CHANGED,
                requestedBy = "test-user",
            )

            // Then
            assertAll(
                { assertTrue(result.isSuccessful) },
                { assertEquals(testCompany, result.notification.company) },
                { assertEquals(AddressType.REGISTERED, result.notification.addressType) },
                { assertEquals(AddressNotificationType.ADDRESS_CHANGED, result.notification.notificationType) },
                { assertEquals(previousAddress, result.notification.previousAddress) },
                { assertEquals(newAddress, result.notification.newAddress) },
                { assertTrue(result.deliveryResults.isNotEmpty()) },
            )

            verify { auditService.logEvent(any(), any(), any(), any()) }
        }

        @Test
        fun `should generate appropriate notification subject`() {
            // Given
            val newAddress = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Test Street",
                city = "Auckland",
                country = "NZ",
                postcode = "1010",
                email = "test@example.com",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressNotificationService.sendAddressChangeNotification(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                previousAddress = null,
                newAddress = newAddress,
                notificationType = AddressNotificationType.ADDRESS_CHANGED,
            )

            // Then
            assertTrue(result.notification.subject.contains("Address Update Confirmation"))
            assertTrue(result.notification.subject.contains(testCompany.companyName))
        }

        @Test
        fun `should generate appropriate notification content for address change`() {
            // Given
            val previousAddress = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Test Street",
                city = "Auckland",
                country = "NZ",
                postcode = "1010",
                email = "test@example.com",
                effectiveFrom = LocalDate.now(),
            )
            val newAddress = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "456 New Street",
                city = "Auckland",
                country = "NZ",
                postcode = "1010",
                email = "test@example.com",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressNotificationService.sendAddressChangeNotification(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                previousAddress = previousAddress,
                newAddress = newAddress,
                notificationType = AddressNotificationType.ADDRESS_CHANGED,
            )

            // Then
            val content = result.notification.content
            assertAll(
                { assertTrue(content.contains(testCompany.companyName)) },
                { assertTrue(content.contains(testCompany.companyNumber)) },
                { assertTrue(content.contains("Previous address")) },
                { assertTrue(content.contains("New address")) },
                { assertTrue(content.contains(previousAddress.getFullAddress())) },
                { assertTrue(content.contains(newAddress.getFullAddress())) },
            )
        }

        @Test
        fun `should handle workflow approval notifications`() {
            // Given
            val newAddress = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Test Street",
                city = "Auckland",
                country = "NZ",
                postcode = "1010",
                email = "test@example.com",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressNotificationService.sendAddressChangeNotification(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                previousAddress = null,
                newAddress = newAddress,
                notificationType = AddressNotificationType.WORKFLOW_APPROVED,
            )

            // Then
            assertAll(
                { assertTrue(result.isSuccessful) },
                { assertEquals(AddressNotificationType.WORKFLOW_APPROVED, result.notification.notificationType) },
                { assertTrue(result.notification.subject.contains("Address Change Approved")) },
                { assertTrue(result.notification.content.contains("has been approved")) },
            )
        }

        @Test
        fun `should handle workflow rejection notifications`() {
            // Given
            val newAddress = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Test Street",
                city = "Auckland",
                country = "NZ",
                postcode = "1010",
                email = "test@example.com",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressNotificationService.sendAddressChangeNotification(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                previousAddress = null,
                newAddress = newAddress,
                notificationType = AddressNotificationType.WORKFLOW_REJECTED,
            )

            // Then
            assertAll(
                { assertTrue(result.isSuccessful) },
                { assertEquals(AddressNotificationType.WORKFLOW_REJECTED, result.notification.notificationType) },
                { assertTrue(result.notification.subject.contains("Address Change Rejected")) },
            )
        }
    }

    @Nested
    @DisplayName("Bulk Notifications")
    inner class BulkNotifications {

        @Test
        fun `should send bulk address change notifications`() {
            // Given
            val addressChanges = listOf(
                AddressChangeNotificationRequest(
                    addressType = AddressType.REGISTERED,
                    previousAddress = Address(
                        company = testCompany,
                        addressType = AddressType.REGISTERED,
                        addressLine1 = "123 Test Street",
                        city = "Auckland",
                        country = "NZ",
                        postcode = "1010",
                        email = "test@example.com",
                        effectiveFrom = LocalDate.now(),
                    ),
                    newAddress = Address(
                        company = testCompany,
                        addressType = AddressType.REGISTERED,
                        addressLine1 = "456 New Street",
                        city = "Auckland",
                        country = "NZ",
                        postcode = "1010",
                        email = "test@example.com",
                        effectiveFrom = LocalDate.now(),
                    ),
                ),
                AddressChangeNotificationRequest(
                    addressType = AddressType.SERVICE,
                    previousAddress = Address(
                        company = testCompany,
                        addressType = AddressType.SERVICE,
                        addressLine1 = "123 Test Street",
                        city = "Auckland",
                        country = "NZ",
                        postcode = "1010",
                        email = "test@example.com",
                        effectiveFrom = LocalDate.now(),
                    ),
                    newAddress = Address(
                        company = testCompany,
                        addressType = AddressType.SERVICE,
                        addressLine1 = "789 Service Street",
                        city = "Auckland",
                        country = "NZ",
                        postcode = "1010",
                        email = "test@example.com",
                        effectiveFrom = LocalDate.now(),
                    ),
                ),
            )

            // When
            val result = addressNotificationService.sendBulkAddressChangeNotifications(
                company = testCompany,
                addressChanges = addressChanges,
                requestedBy = "test-user",
            )

            // Then
            assertAll(
                { assertTrue(result.isSuccessful) },
                { assertEquals(testCompany, result.company) },
                { assertEquals(2, result.individualResults.size) },
                { assertEquals(2, result.getTotalNotificationsSent()) },
                { assertEquals(2, result.getSuccessfulNotifications()) },
                { assertEquals(0, result.getFailedNotifications()) },
                { assertEquals(1.0, result.getOverallSuccessRate()) },
                { assertTrue(result.summaryResult.isSuccessful) },
            )

            verify { auditService.logEvent(any(), any(), any(), any()) }
        }

        @Test
        fun `should generate bulk notification summary`() {
            // Given
            val addressChanges = listOf(
                AddressChangeNotificationRequest(
                    addressType = AddressType.REGISTERED,
                    previousAddress = null,
                    newAddress = Address(
                        company = testCompany,
                        addressType = AddressType.REGISTERED,
                        addressLine1 = "123 Test Street",
                        city = "Auckland",
                        country = "NZ",
                        postcode = "1010",
                        email = "test@example.com",
                        effectiveFrom = LocalDate.now(),
                    ),
                ),
            )

            // When
            val result = addressNotificationService.sendBulkAddressChangeNotifications(
                company = testCompany,
                addressChanges = addressChanges,
            )

            // Then
            val summaryNotification = result.summaryResult
            assertTrue(summaryNotification.isSuccessful)
        }
    }

    @Nested
    @DisplayName("Compliance Notifications")
    inner class ComplianceNotifications {

        @Test
        fun `should send compliance notifications`() {
            // Given
            val complianceIssues = listOf(
                AddressComplianceIssue(
                    addressType = AddressType.REGISTERED,
                    description = "Missing postcode for registered address",
                    severity = AddressComplianceSeverity.MEDIUM,
                    dueDate = LocalDate.now().plusDays(30),
                    identifiedAt = java.time.LocalDateTime.now(),
                ),
                AddressComplianceIssue(
                    addressType = AddressType.SERVICE,
                    description = "Service address required",
                    severity = AddressComplianceSeverity.HIGH,
                    dueDate = LocalDate.now().plusDays(7),
                    identifiedAt = java.time.LocalDateTime.now(),
                ),
            )

            // When
            val results = addressNotificationService.sendComplianceNotifications(
                company = testCompany,
                complianceIssues = complianceIssues,
            )

            // Then
            assertAll(
                { assertEquals(2, results.size) },
                { assertTrue(results.all { it.isSuccessful }) },
                {
                    assertTrue(
                        results.all { it.notification.notificationType == AddressNotificationType.COMPLIANCE_REQUIRED },
                    )
                },
                { assertTrue(results.all { it.notification.subject.contains("Address Compliance Notice") }) },
            )
        }

        @Test
        fun `should identify overdue compliance issues`() {
            // Given
            val overdueIssue = AddressComplianceIssue(
                addressType = AddressType.REGISTERED,
                description = "Test issue",
                severity = AddressComplianceSeverity.HIGH,
                dueDate = LocalDate.now().minusDays(1),
                identifiedAt = java.time.LocalDateTime.now(),
            )

            // Then
            assertTrue(overdueIssue.isOverdue())
            assertTrue(overdueIssue.getDaysUntilDue() < 0)
        }

        @Test
        fun `should calculate days until due for compliance issues`() {
            // Given
            val futureIssue = AddressComplianceIssue(
                addressType = AddressType.REGISTERED,
                description = "Test issue",
                severity = AddressComplianceSeverity.MEDIUM,
                dueDate = LocalDate.now().plusDays(15),
                identifiedAt = java.time.LocalDateTime.now(),
            )

            // Then
            assertFalse(futureIssue.isOverdue())
            assertEquals(15, futureIssue.getDaysUntilDue())
        }
    }

    @Nested
    @DisplayName("Notification Preferences")
    inner class NotificationPreferences {

        @Test
        fun `should validate notification preferences successfully`() {
            // Given
            val preferences = AddressNotificationPreferences(
                emailAddresses = listOf("test@example.com", "contact@company.com"),
                phoneNumbers = listOf("+64 9 123 4567"),
                postalAddress = testAddress,
                deliveryMethods = listOf(AddressDeliveryMethod.EMAIL, AddressDeliveryMethod.SMS),
                notificationTypes = listOf(AddressNotificationType.ADDRESS_CHANGED),
            )

            // When
            val validation = addressNotificationService.validateNotificationPreferences(testCompany, preferences)

            // Then
            assertAll(
                { assertTrue(validation.isValid) },
                { assertTrue(validation.errors.isEmpty()) },
                { assertFalse(validation.hasErrors()) },
            )
        }

        @Test
        fun `should reject invalid email addresses`() {
            // Given
            val preferences = AddressNotificationPreferences(
                emailAddresses = listOf("invalid-email", "another-invalid"),
                phoneNumbers = emptyList(),
                postalAddress = null,
                deliveryMethods = listOf(AddressDeliveryMethod.EMAIL),
                notificationTypes = listOf(AddressNotificationType.ADDRESS_CHANGED),
            )

            // When
            val validation = addressNotificationService.validateNotificationPreferences(testCompany, preferences)

            // Then
            assertAll(
                { assertFalse(validation.isValid) },
                { assertTrue(validation.hasErrors()) },
                { assertEquals(2, validation.errors.size) },
                { assertTrue(validation.errors.all { it.contains("Invalid email address") }) },
            )
        }

        @Test
        fun `should reject invalid phone numbers`() {
            // Given
            val preferences = AddressNotificationPreferences(
                emailAddresses = listOf("test@example.com"),
                phoneNumbers = listOf("123", "invalid-phone"),
                postalAddress = null,
                deliveryMethods = listOf(AddressDeliveryMethod.EMAIL, AddressDeliveryMethod.SMS),
                notificationTypes = listOf(AddressNotificationType.ADDRESS_CHANGED),
            )

            // When
            val validation = addressNotificationService.validateNotificationPreferences(testCompany, preferences)

            // Then
            assertAll(
                { assertFalse(validation.isValid) },
                { assertTrue(validation.hasErrors()) },
                { assertEquals(2, validation.errors.size) },
                { assertTrue(validation.errors.all { it.contains("Invalid phone number") }) },
            )
        }

        @Test
        fun `should require at least one delivery method`() {
            // Given
            val preferences = AddressNotificationPreferences(
                emailAddresses = emptyList(),
                phoneNumbers = emptyList(),
                postalAddress = null,
                deliveryMethods = emptyList(),
                notificationTypes = listOf(AddressNotificationType.ADDRESS_CHANGED),
            )

            // When
            val validation = addressNotificationService.validateNotificationPreferences(testCompany, preferences)

            // Then
            assertAll(
                { assertFalse(validation.isValid) },
                { assertTrue(validation.hasErrors()) },
                {
                    assertTrue(
                        validation.errors.any {
                            it.contains("At least one notification delivery method must be specified")
                        },
                    )
                },
                {
                    assertTrue(
                        validation.warnings.any { it.contains("No delivery methods specified") },
                    )
                },
            )
        }

        @Test
        fun `should check delivery method capabilities`() {
            // Given
            val preferences = AddressNotificationPreferences(
                emailAddresses = listOf("test@example.com"),
                phoneNumbers = listOf("+64 9 123 4567"),
                postalAddress = testAddress,
                deliveryMethods = listOf(
                    AddressDeliveryMethod.EMAIL,
                    AddressDeliveryMethod.SMS,
                    AddressDeliveryMethod.POST,
                ),
                notificationTypes = listOf(AddressNotificationType.ADDRESS_CHANGED),
            )

            // Then
            assertAll(
                { assertTrue(preferences.hasEmailNotifications()) },
                { assertTrue(preferences.hasSmsNotifications()) },
                { assertTrue(preferences.hasPostalNotifications()) },
            )
        }
    }

    @Nested
    @DisplayName("Delivery Results")
    inner class AddressDeliveryResults {

        @Test
        fun `should track successful delivery results`() {
            // Given
            val recipient = AddressNotificationRecipient(
                type = AddressRecipientType.COMPANY_CONTACT,
                deliveryMethod = AddressDeliveryMethod.EMAIL,
                address = "test@example.com",
                name = "Test Contact",
            )

            val deliveryResult = AddressDeliveryResult(
                recipient = recipient,
                isSuccessful = true,
                deliveredAt = java.time.LocalDateTime.now(),
                attemptedAt = java.time.LocalDateTime.now(),
            )

            // Then
            assertAll(
                { assertTrue(deliveryResult.isSuccessful) },
                { assertEquals(recipient, deliveryResult.recipient) },
                { assertTrue(deliveryResult.getDeliveryStatus().contains("Delivered to")) },
                { assertTrue(deliveryResult.getDeliveryStatus().contains("test@example.com")) },
            )
        }

        @Test
        fun `should track failed delivery results`() {
            // Given
            val recipient = AddressNotificationRecipient(
                type = AddressRecipientType.COMPANY_CONTACT,
                deliveryMethod = AddressDeliveryMethod.EMAIL,
                address = "invalid@example.com",
                name = "Test Contact",
            )

            val deliveryResult = AddressDeliveryResult(
                recipient = recipient,
                isSuccessful = false,
                attemptedAt = java.time.LocalDateTime.now(),
                error = "Email address not found",
            )

            // Then
            assertAll(
                { assertFalse(deliveryResult.isSuccessful) },
                { assertEquals("Email address not found", deliveryResult.error) },
                { assertTrue(deliveryResult.getDeliveryStatus().contains("Failed to deliver")) },
                { assertTrue(deliveryResult.getDeliveryStatus().contains("Email address not found")) },
            )
        }

        @Test
        fun `should calculate delivery rates correctly`() {
            // Given
            val successfulDelivery = AddressDeliveryResult(
                recipient = AddressNotificationRecipient(
                    type = AddressRecipientType.COMPANY_CONTACT,
                    deliveryMethod = AddressDeliveryMethod.EMAIL,
                    address = "success@example.com",
                    name = "Success",
                ),
                isSuccessful = true,
                attemptedAt = java.time.LocalDateTime.now(),
            )

            val failedDelivery = AddressDeliveryResult(
                recipient = AddressNotificationRecipient(
                    type = AddressRecipientType.COMPANY_CONTACT,
                    deliveryMethod = AddressDeliveryMethod.EMAIL,
                    address = "failed@example.com",
                    name = "Failed",
                ),
                isSuccessful = false,
                attemptedAt = java.time.LocalDateTime.now(),
                error = "Delivery failed",
            )

            val notification = AddressNotification(
                id = "test-id",
                company = testCompany,
                addressType = AddressType.REGISTERED,
                notificationType = AddressNotificationType.ADDRESS_CHANGED,
                subject = "Test",
                content = "Test content",
                previousAddress = null,
                newAddress = testAddress,
                requestedBy = "test",
                createdAt = java.time.LocalDateTime.now(),
            )

            val result = AddressNotificationResult(
                notification = notification,
                deliveryResults = listOf(successfulDelivery, failedDelivery),
                isSuccessful = false,
                sentAt = java.time.LocalDateTime.now(),
            )

            // Then
            assertAll(
                { assertEquals(1, result.getSuccessfulDeliveries().size) },
                { assertEquals(1, result.getFailedDeliveries().size) },
                { assertEquals(0.5, result.getDeliveryRate()) },
                { assertFalse(result.isSuccessful) },
            )
        }
    }

    @Nested
    @DisplayName("Reminder Notifications")
    inner class ReminderNotifications {

        @Test
        fun `should send reminder notifications for pending changes`() {
            // When
            val results = addressNotificationService.sendAddressChangeReminders()

            // Then - currently returns empty list as placeholder
            assertTrue(results.isEmpty())
        }
    }
}
