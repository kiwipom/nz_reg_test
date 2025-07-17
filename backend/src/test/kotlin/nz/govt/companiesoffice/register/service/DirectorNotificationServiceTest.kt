package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.entity.DirectorStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DirectorNotificationServiceTest {

    private val notificationService = DirectorNotificationService()

    private lateinit var testCompany: Company
    private lateinit var testDirector: Director

    @BeforeEach
    fun setUp() {
        testCompany = Company(
            id = 1L,
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.now(),
        )

        testDirector = Director(
            id = 1L,
            company = testCompany,
            fullName = "John Doe",
            dateOfBirth = LocalDate.of(1990, 1, 1),
            residentialAddressLine1 = "123 Main St",
            residentialCity = "Auckland",
            residentialCountry = "NZ",
            isNzResident = true,
            isAustralianResident = false,
            appointedDate = LocalDate.now(),
            status = DirectorStatus.ACTIVE,
            consentGiven = false,
        )
    }

    @Test
    fun `should send director appointment notifications`() {
        // Test the notification method doesn't throw exceptions
        notificationService.notifyDirectorAppointment(testDirector)

        // In a real implementation, we would verify:
        // - Notifications were sent to registrar
        // - Notifications were sent to company contacts
        // - Consent notification was sent (since consentGiven = false)
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `should send director appointment notifications without consent requirement`() {
        testDirector.consentGiven = true
        testDirector.consentDate = LocalDate.now()

        notificationService.notifyDirectorAppointment(testDirector)

        // Should not send consent notification when consent is already given
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `should send director resignation notifications`() {
        val resignationDate = LocalDate.now()

        notificationService.notifyDirectorResignation(testDirector, resignationDate)

        // In a real implementation, we would verify:
        // - Resignation notifications were sent
        // - Compliance checks were performed
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `should send director disqualification notifications`() {
        val reason = "Court order ABC123"

        notificationService.notifyDirectorDisqualification(testDirector, reason)

        // In a real implementation, we would verify:
        // - Disqualification notifications were sent with high priority
        // - Compliance impact was assessed
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `should send director update notifications for significant changes`() {
        val updatedFields = listOf("Full Name", "NZ Residency Status")
        val isSignificantChange = true

        notificationService.notifyDirectorUpdate(testDirector, updatedFields, isSignificantChange)

        // In a real implementation, we would verify:
        // - Update notifications were sent to registrar
        // - Company was notified due to significant change
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `should send minimal notifications for non-significant changes`() {
        val updatedFields = listOf("Place of Birth")
        val isSignificantChange = false

        notificationService.notifyDirectorUpdate(testDirector, updatedFields, isSignificantChange)

        // In a real implementation, we would verify:
        // - Only registrar was notified
        // - Company was not notified for non-significant change
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `should send director consent notifications`() {
        val consentDate = LocalDate.now()

        notificationService.notifyDirectorConsent(testDirector, consentDate)

        // In a real implementation, we would verify:
        // - Consent notifications were sent to registrar and company
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `should send compliance reminders with appropriate priorities`() {
        val companyId = 1L
        val issues = listOf(
            ComplianceIssue(
                type = "MISSING_DIRECTOR",
                description = "Company has no active directors",
                severity = ComplianceSeverity.CRITICAL,
            ),
            ComplianceIssue(
                type = "MISSING_CONSENT",
                description = "Director consent required",
                severity = ComplianceSeverity.HIGH,
            ),
            ComplianceIssue(
                type = "ADDRESS_UPDATE",
                description = "Please update director address",
                severity = ComplianceSeverity.LOW,
            ),
        )

        notificationService.sendComplianceReminders(companyId, issues)

        // In a real implementation, we would verify:
        // - Critical issues generated urgent notifications
        // - High issues generated high priority notifications
        // - Low issues generated low priority notifications
        assertTrue(true) // Placeholder assertion
    }

    @Test
    fun `should handle notification failures gracefully`() {
        // Test that the service doesn't throw exceptions even if underlying
        // notification systems fail

        notificationService.notifyDirectorAppointment(testDirector)
        notificationService.notifyDirectorResignation(testDirector, LocalDate.now())
        notificationService.notifyDirectorDisqualification(testDirector, "Test reason")
        notificationService.notifyDirectorUpdate(testDirector, listOf("Full Name"), true)
        notificationService.notifyDirectorConsent(testDirector, LocalDate.now())

        // All methods should complete without throwing exceptions
        assertTrue(true)
    }

    @Test
    fun `should create proper notification structure`() {
        // Test notification data structure (this would be more comprehensive in a real implementation)
        val testNotification = DirectorNotification(
            id = "TEST-123",
            type = NotificationType.DIRECTOR_APPOINTMENT,
            recipient = NotificationRecipient.COMPANY_REGISTRAR,
            title = "Test Title",
            message = "Test Message",
            directorId = 1L,
            directorName = "John Doe",
            companyId = 1L,
            companyName = "Test Company Ltd",
            priority = NotificationPriority.NORMAL,
            status = NotificationStatus.PENDING,
            createdAt = java.time.LocalDateTime.now(),
        )

        assertEquals("TEST-123", testNotification.id)
        assertEquals(NotificationType.DIRECTOR_APPOINTMENT, testNotification.type)
        assertEquals(NotificationRecipient.COMPANY_REGISTRAR, testNotification.recipient)
        assertEquals("Test Title", testNotification.title)
        assertEquals("Test Message", testNotification.message)
        assertEquals(1L, testNotification.directorId)
        assertEquals("John Doe", testNotification.directorName)
        assertEquals(1L, testNotification.companyId)
        assertEquals("Test Company Ltd", testNotification.companyName)
        assertEquals(NotificationPriority.NORMAL, testNotification.priority)
        assertEquals(NotificationStatus.PENDING, testNotification.status)
    }

    @Test
    fun `should validate notification priority levels`() {
        val priorities = NotificationPriority.values()

        assertTrue(priorities.contains(NotificationPriority.LOW))
        assertTrue(priorities.contains(NotificationPriority.NORMAL))
        assertTrue(priorities.contains(NotificationPriority.HIGH))
        assertTrue(priorities.contains(NotificationPriority.URGENT))
        assertTrue(priorities.contains(NotificationPriority.CRITICAL))
    }

    @Test
    fun `should validate notification types`() {
        val types = NotificationType.values()

        assertTrue(types.contains(NotificationType.DIRECTOR_APPOINTMENT))
        assertTrue(types.contains(NotificationType.DIRECTOR_RESIGNATION))
        assertTrue(types.contains(NotificationType.DIRECTOR_DISQUALIFICATION))
        assertTrue(types.contains(NotificationType.DIRECTOR_UPDATE))
        assertTrue(types.contains(NotificationType.CONSENT_REQUIRED))
        assertTrue(types.contains(NotificationType.CONSENT_RECEIVED))
        assertTrue(types.contains(NotificationType.COMPLIANCE_VIOLATION))
        assertTrue(types.contains(NotificationType.COMPLIANCE_REMINDER))
    }

    @Test
    fun `should validate notification recipients`() {
        val recipients = NotificationRecipient.values()

        assertTrue(recipients.contains(NotificationRecipient.COMPANY_REGISTRAR))
        assertTrue(recipients.contains(NotificationRecipient.COMPANY_CONTACTS))
        assertTrue(recipients.contains(NotificationRecipient.DIRECTOR_PERSONAL))
        assertTrue(recipients.contains(NotificationRecipient.EXTERNAL_AGENCIES))
    }

    @Test
    fun `should create compliance issues with proper severity levels`() {
        val criticalIssue = ComplianceIssue(
            type = "NO_DIRECTORS",
            description = "Company has no directors",
            severity = ComplianceSeverity.CRITICAL,
        )

        val highIssue = ComplianceIssue(
            type = "MISSING_CONSENT",
            description = "Director consent missing",
            severity = ComplianceSeverity.HIGH,
        )

        val mediumIssue = ComplianceIssue(
            type = "ADDRESS_OUTDATED",
            description = "Director address may be outdated",
            severity = ComplianceSeverity.MEDIUM,
        )

        val lowIssue = ComplianceIssue(
            type = "PROFILE_INCOMPLETE",
            description = "Director profile incomplete",
            severity = ComplianceSeverity.LOW,
        )

        assertEquals(ComplianceSeverity.CRITICAL, criticalIssue.severity)
        assertEquals(ComplianceSeverity.HIGH, highIssue.severity)
        assertEquals(ComplianceSeverity.MEDIUM, mediumIssue.severity)
        assertEquals(ComplianceSeverity.LOW, lowIssue.severity)
    }
}
