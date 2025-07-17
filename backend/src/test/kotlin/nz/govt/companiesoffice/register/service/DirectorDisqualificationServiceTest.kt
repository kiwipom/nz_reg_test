package nz.govt.companiesoffice.register.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.entity.DirectorStatus
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.DirectorRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectorDisqualificationServiceTest {

    private val directorRepository = mockk<DirectorRepository>()
    private val disqualificationService = DirectorDisqualificationService(directorRepository)

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
        )
    }

    @Test
    fun `should allow appointment of eligible person`() {
        every { directorRepository.findDisqualifiedDirectorsByName("John Doe") } returns emptyList()

        val result = disqualificationService.checkDisqualificationStatus(
            fullName = "John Doe",
            dateOfBirth = LocalDate.of(1990, 1, 1),
        )

        assertFalse(result.isDisqualified)
        assertTrue(result.eligibleForAppointment)
        assertTrue(result.disqualifications.isEmpty())
    }

    @Test
    fun `should detect age-based disqualification for under 18`() {
        val underageDate = LocalDate.now().minusYears(17)
        every { directorRepository.findDisqualifiedDirectorsByName("Jane Minor") } returns emptyList()

        val result = disqualificationService.checkDisqualificationStatus(
            fullName = "Jane Minor",
            dateOfBirth = underageDate,
        )

        assertTrue(result.isDisqualified)
        assertFalse(result.eligibleForAppointment)
        assertEquals(1, result.disqualifications.size)
        assertEquals(DisqualificationType.AGE_RESTRICTION, result.disqualifications[0].type)
        assertTrue(result.disqualifications[0].reason.contains("under 18"))
    }

    @Test
    fun `should detect existing disqualification from database`() {
        val disqualifiedDirector = Director(
            id = 2L,
            company = testCompany,
            fullName = "John Doe",
            dateOfBirth = LocalDate.of(1985, 5, 15),
            residentialAddressLine1 = "456 Oak Ave",
            residentialCity = "Wellington",
            residentialCountry = "NZ",
            isNzResident = true,
            isAustralianResident = false,
            appointedDate = LocalDate.now().minusYears(2),
            status = DirectorStatus.DISQUALIFIED,
        )

        every { directorRepository.findDisqualifiedDirectorsByName("John Doe") } returns listOf(disqualifiedDirector)

        val result = disqualificationService.checkDisqualificationStatus(
            fullName = "John Doe",
            dateOfBirth = LocalDate.of(1990, 1, 1),
        )

        assertTrue(result.isDisqualified)
        assertFalse(result.eligibleForAppointment)
        assertEquals(1, result.disqualifications.size)
        assertEquals(DisqualificationType.DATABASE_RECORD, result.disqualifications[0].type)
    }

    @Test
    fun `should disqualify director successfully`() {
        // Ensure director starts as ACTIVE
        testDirector.status = DirectorStatus.ACTIVE
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 2
        every { directorRepository.countResidentDirectorsByCompanyId(1L) } returns 2
        val savedDirector = Director(
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
            status = DirectorStatus.DISQUALIFIED,
        )
        every { directorRepository.save(any<Director>()) } returns savedDirector

        val result = disqualificationService.disqualifyDirector(
            directorId = 1L,
            disqualificationType = DisqualificationType.COURT_ORDER,
            reason = "Court order ABC123",
        )

        assertEquals(DirectorStatus.DISQUALIFIED, result.director.status)
        assertEquals(DisqualificationType.COURT_ORDER, result.disqualificationRecord.type)
        assertEquals("Court order ABC123", result.disqualificationRecord.reason)
        assertTrue(result.complianceImpact.meetsMinimumDirectors)
        assertTrue(result.complianceImpact.meetsResidencyRequirements)

        verify { directorRepository.save(any<Director>()) }
    }

    @Test
    fun `should throw validation exception when disqualifying already disqualified director`() {
        val disqualifiedDirector = Director(
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
            status = DirectorStatus.DISQUALIFIED,
        )
        every { directorRepository.findById(1L) } returns Optional.of(disqualifiedDirector)

        val exception = assertThrows<ValidationException> {
            disqualificationService.disqualifyDirector(
                directorId = 1L,
                disqualificationType = DisqualificationType.COURT_ORDER,
                reason = "Test reason",
            )
        }

        assertEquals("status", exception.field)
        assertTrue(exception.message!!.contains("already disqualified"))
    }

    @Test
    fun `should throw validation exception when disqualification would violate minimum directors`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 1

        val exception = assertThrows<ValidationException> {
            disqualificationService.disqualifyDirector(
                directorId = 1L,
                disqualificationType = DisqualificationType.COMPLIANCE_BREACH,
                reason = "Test reason",
            )
        }

        assertEquals("disqualification", exception.field)
        assertTrue(exception.message!!.contains("at least one active director"))
    }

    @Test
    fun `should throw validation exception when disqualification would violate residency requirements`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 2
        every { directorRepository.countResidentDirectorsByCompanyId(1L) } returns 1

        val exception = assertThrows<ValidationException> {
            disqualificationService.disqualifyDirector(
                directorId = 1L,
                disqualificationType = DisqualificationType.BANKRUPTCY,
                reason = "Bankruptcy proceedings",
            )
        }

        assertEquals("disqualification", exception.field)
        assertTrue(exception.message!!.contains("NZ/Australian resident director"))
    }

    @Test
    fun `should lift disqualification successfully`() {
        // Create a separate disqualified director instance
        val disqualifiedDirector = Director(
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
            status = DirectorStatus.DISQUALIFIED,
        )
        every { directorRepository.findById(1L) } returns Optional.of(disqualifiedDirector)
        val activeDirector = Director(
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
        )
        every { directorRepository.save(any<Director>()) } returns activeDirector

        val result = disqualificationService.liftDisqualification(
            directorId = 1L,
            reason = "Court order lifted",
        )

        assertEquals(DirectorStatus.ACTIVE, result.status)
        verify { directorRepository.save(any<Director>()) }
    }

    @Test
    fun `should throw validation exception when lifting disqualification from non-disqualified director`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector) // ACTIVE status

        val exception = assertThrows<ValidationException> {
            disqualificationService.liftDisqualification(
                directorId = 1L,
                reason = "Test reason",
            )
        }

        assertEquals("status", exception.field)
        assertTrue(exception.message!!.contains("not currently disqualified"))
    }

    @Test
    fun `should validate appointment eligibility successfully for eligible person`() {
        every { directorRepository.findDisqualifiedDirectorsByName("John Doe") } returns emptyList()

        // Should not throw any exceptions
        disqualificationService.validateAppointmentEligibility(
            fullName = "John Doe",
            dateOfBirth = LocalDate.of(1990, 1, 1),
            companyId = 1L,
        )
    }

    @Test
    fun `should throw validation exception for disqualified person appointment`() {
        val disqualifiedDirector = testDirector.apply { status = DirectorStatus.DISQUALIFIED }
        every { directorRepository.findDisqualifiedDirectorsByName("John Doe") } returns listOf(disqualifiedDirector)

        val exception = assertThrows<ValidationException> {
            disqualificationService.validateAppointmentEligibility(
                fullName = "John Doe",
                dateOfBirth = LocalDate.of(1990, 1, 1),
                companyId = 1L,
            )
        }

        assertEquals("disqualification", exception.field)
        assertTrue(exception.message!!.contains("person is disqualified"))
    }

    @Test
    fun `should get comprehensive disqualification info`() {
        val disqualifiedDirector = testDirector.apply { status = DirectorStatus.DISQUALIFIED }
        every { directorRepository.findById(1L) } returns Optional.of(disqualifiedDirector)

        val info = disqualificationService.getDisqualificationInfo(1L)

        assertEquals(1L, info.directorId)
        assertEquals("John Doe", info.directorName)
        assertEquals(DirectorStatus.DISQUALIFIED, info.currentStatus)
        assertTrue(info.isCurrentlyDisqualified)
    }

    @Test
    fun `should throw validation exception for empty disqualification reason`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)

        val exception = assertThrows<ValidationException> {
            disqualificationService.disqualifyDirector(
                directorId = 1L,
                disqualificationType = DisqualificationType.OTHER,
                reason = "", // Empty reason
            )
        }

        assertEquals("reason", exception.field)
        assertTrue(exception.message!!.contains("required"))
    }

    @Test
    fun `should handle multiple disqualification types correctly`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 3
        every { directorRepository.countResidentDirectorsByCompanyId(1L) } returns 2
        every { directorRepository.save(any<Director>()) } returns testDirector.apply {
            status = DirectorStatus.DISQUALIFIED
        }

        // Test each disqualification type
        val types = listOf(
            DisqualificationType.COURT_ORDER to "Court order",
            DisqualificationType.BANKRUPTCY to "Bankruptcy filing",
            DisqualificationType.COMPLIANCE_BREACH to "Failed to file returns",
            DisqualificationType.VOLUNTARY to "Voluntary disqualification",
            DisqualificationType.OTHER to "Other statutory reason",
        )

        types.forEach { (type, reason) ->
            // Reset director status for each test
            testDirector.status = DirectorStatus.ACTIVE

            val result = disqualificationService.disqualifyDirector(
                directorId = 1L,
                disqualificationType = type,
                reason = reason,
            )

            assertEquals(type, result.disqualificationRecord.type)
            assertEquals(reason, result.disqualificationRecord.reason)
        }
    }
}
