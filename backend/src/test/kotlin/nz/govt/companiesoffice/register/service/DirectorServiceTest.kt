package nz.govt.companiesoffice.register.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.entity.DirectorStatus
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
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

class DirectorServiceTest {

    private val directorRepository = mockk<DirectorRepository>()
    private val auditService = mockk<AuditService>(relaxed = true)
    private val residencyValidationService = mockk<ResidencyValidationService>(relaxed = true)
    private val disqualificationService = mockk<DirectorDisqualificationService>(relaxed = true)
    private val notificationService = mockk<DirectorNotificationService>(relaxed = true)
    private val directorService = DirectorService(
        directorRepository,
        auditService,
        residencyValidationService,
        disqualificationService,
        notificationService,
    )

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
    fun `should appoint director successfully`() {
        every { directorRepository.existsByCompanyIdAndFullNameIgnoreCase(any(), any()) } returns false
        every { directorRepository.save(any<Director>()) } returns testDirector

        val result = directorService.appointDirector(testDirector)

        assertEquals(testDirector, result)
        verify { directorRepository.save(testDirector) }
        verify { auditService.logDirectorAppointment(1L, 1L, "John Doe") }
    }

    @Test
    fun `should throw validation exception when director already exists`() {
        every { directorRepository.existsByCompanyIdAndFullNameIgnoreCase(1L, "John Doe") } returns true

        val exception = assertThrows<ValidationException> {
            directorService.appointDirector(testDirector)
        }

        assertEquals("fullName", exception.field)
        assertTrue(exception.message!!.contains("already exists"))
    }

    @Test
    fun `should throw validation exception when director is not NZ or Australian resident`() {
        val nonResidentDirector = Director(
            id = 2L,
            company = testCompany,
            fullName = "Jane Smith",
            dateOfBirth = LocalDate.of(1985, 5, 15),
            residentialAddressLine1 = "456 Oak Ave",
            residentialCity = "London",
            residentialCountry = "UK",
            isNzResident = false,
            isAustralianResident = false,
            appointedDate = LocalDate.now(),
            status = DirectorStatus.ACTIVE,
        )

        every { directorRepository.existsByCompanyIdAndFullNameIgnoreCase(any(), any()) } returns false
        every {
            residencyValidationService.validateDirectorResidency(any())
        } throws ValidationException("residency", "Director must be NZ or Australian resident")

        assertThrows<ValidationException> {
            directorService.appointDirector(nonResidentDirector)
        }
    }

    @Test
    fun `should resign director successfully`() {
        val resignationDate = LocalDate.now()
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 2
        every { directorRepository.countResidentDirectorsByCompanyId(1L) } returns 2

        val resignedDirector = Director(
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
            status = DirectorStatus.RESIGNED,
            resignedDate = resignationDate,
        )
        every { directorRepository.save(any<Director>()) } returns resignedDirector

        val result = directorService.resignDirector(1L, resignationDate)

        assertEquals(DirectorStatus.RESIGNED, result.status)
        assertEquals(resignationDate, result.resignedDate)
        verify { auditService.logDirectorResignation(1L, 1L, "John Doe", resignationDate) }
    }

    @Test
    fun `should throw validation exception when resigning director would violate minimum directors requirement`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 1

        val exception = assertThrows<ValidationException> {
            directorService.resignDirector(1L)
        }

        assertEquals("resignation", exception.field)
        assertTrue(exception.message!!.contains("at least one director"))
    }

    @Test
    fun `should throw validation exception when resigning would violate residency requirement`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 2
        every { directorRepository.countResidentDirectorsByCompanyId(1L) } returns 1
        every {
            residencyValidationService.validateResignationWontViolateResidency(any())
        } throws ValidationException("resignation", "Cannot resign - would violate resident director requirement")

        val exception = assertThrows<ValidationException> {
            directorService.resignDirector(1L)
        }

        assertEquals("resignation", exception.field)
        assertTrue(exception.message!!.contains("resident director"))
    }

    @Test
    fun `should throw validation exception when trying to resign inactive director`() {
        val resignedDirector = Director(
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
            status = DirectorStatus.RESIGNED,
        )
        every { directorRepository.findById(1L) } returns Optional.of(resignedDirector)

        val exception = assertThrows<ValidationException> {
            directorService.resignDirector(1L)
        }

        assertEquals("status", exception.field)
        assertTrue(exception.message!!.contains("Only active directors"))
    }

    @Test
    fun `should update director successfully`() {
        val updatedDirector = Director(
            id = 1L,
            company = testCompany,
            fullName = "Jane Doe",
            dateOfBirth = LocalDate.of(1990, 1, 1),
            residentialAddressLine1 = "123 Main St",
            residentialCity = "Auckland",
            residentialCountry = "NZ",
            isNzResident = true,
            isAustralianResident = false,
            appointedDate = LocalDate.now(),
            status = DirectorStatus.ACTIVE,
        )
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.save(any<Director>()) } returns testDirector.apply { fullName = "Jane Doe" }

        val result = directorService.updateDirector(1L, updatedDirector)

        assertEquals("Jane Doe", result.fullName)
        verify { auditService.logDirectorUpdate(1L, 1L, "Jane Doe") }
    }

    @Test
    fun `should give director consent successfully`() {
        val consentDate = LocalDate.now()
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)

        val consentedDirector = Director(
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
            consentGiven = true,
            consentDate = consentDate,
        )
        every { directorRepository.save(any<Director>()) } returns consentedDirector

        val result = directorService.giveDirectorConsent(1L, consentDate)

        assertTrue(result.consentGiven)
        assertEquals(consentDate, result.consentDate)
        verify { auditService.logDirectorConsent(1L, 1L, "John Doe", consentDate) }
    }

    @Test
    fun `should throw validation exception when giving consent to director who already consented`() {
        val consentedDirector = Director(
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
            consentGiven = true,
        )
        every { directorRepository.findById(1L) } returns Optional.of(consentedDirector)

        val exception = assertThrows<ValidationException> {
            directorService.giveDirectorConsent(1L)
        }

        assertEquals("consent", exception.field)
        assertTrue(exception.message!!.contains("already given consent"))
    }

    @Test
    fun `should disqualify director successfully`() {
        val reason = "Court order"
        val disqualifiedDirector = testDirector.apply { status = DirectorStatus.DISQUALIFIED }

        val disqualificationRecord = DisqualificationRecord(
            directorId = 1L,
            directorName = "John Doe",
            companyId = 1L,
            type = DisqualificationType.COURT_ORDER,
            reason = reason,
            effectiveDate = LocalDate.now(),
            status = DisqualificationStatus.ACTIVE,
        )

        val complianceImpact = ComplianceImpact(
            remainingActiveDirectors = 1L,
            remainingResidentDirectors = 1L,
            meetsMinimumDirectors = true,
            meetsResidencyRequirements = true,
        )

        val disqualificationResult = DisqualificationResult(
            director = disqualifiedDirector,
            disqualificationRecord = disqualificationRecord,
            complianceImpact = complianceImpact,
        )

        every {
            disqualificationService.disqualifyDirector(1L, DisqualificationType.OTHER, reason)
        } returns disqualificationResult

        val result = directorService.disqualifyDirector(1L, reason)

        assertEquals(DirectorStatus.DISQUALIFIED, result.status)
        verify { auditService.logDirectorDisqualification(1L, 1L, "John Doe", reason) }
    }

    @Test
    fun `should throw validation exception when disqualifying already disqualified director`() {
        every {
            disqualificationService.disqualifyDirector(1L, DisqualificationType.OTHER, "Test reason")
        } throws ValidationException("status", "Director is already disqualified")

        val exception = assertThrows<ValidationException> {
            directorService.disqualifyDirector(1L, "Test reason")
        }

        assertEquals("status", exception.field)
        assertTrue(exception.message!!.contains("already disqualified"))
    }

    @Test
    fun `should get director by id successfully`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)

        val result = directorService.getDirectorById(1L)

        assertEquals(testDirector, result)
    }

    @Test
    fun `should throw resource not found exception when director does not exist`() {
        every { directorRepository.findById(1L) } returns Optional.empty()

        val exception = assertThrows<ResourceNotFoundException> {
            directorService.getDirectorById(1L)
        }

        assertTrue(exception.message!!.contains("Director not found"))
    }

    @Test
    fun `should get directors by company`() {
        val directors = listOf(testDirector)
        every { directorRepository.findByCompanyId(1L) } returns directors

        val result = directorService.getDirectorsByCompany(1L)

        assertEquals(directors, result)
    }

    @Test
    fun `should get active directors by company`() {
        val directors = listOf(testDirector)
        every { directorRepository.findByCompanyIdAndStatus(1L, DirectorStatus.ACTIVE) } returns directors

        val result = directorService.getActiveDirectorsByCompany(1L)

        assertEquals(directors, result)
    }

    @Test
    fun `should validate company director compliance`() {
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 2
        every { directorRepository.countResidentDirectorsByCompanyId(1L) } returns 1
        every { directorRepository.findDirectorsRequiringConsent(1L) } returns emptyList()

        val compliance = directorService.validateCompanyDirectorCompliance(1L)

        assertTrue(compliance["hasMinimumDirectors"]!!)
        assertTrue(compliance["hasResidentDirector"]!!)
        assertTrue(compliance["allDirectorsHaveConsent"]!!)
    }

    @Test
    fun `should identify non-compliance when insufficient directors`() {
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 0
        every { directorRepository.countResidentDirectorsByCompanyId(1L) } returns 0
        every { directorRepository.findDirectorsRequiringConsent(1L) } returns listOf(testDirector)

        val compliance = directorService.validateCompanyDirectorCompliance(1L)

        assertFalse(compliance["hasMinimumDirectors"]!!)
        assertFalse(compliance["hasResidentDirector"]!!)
        assertFalse(compliance["allDirectorsHaveConsent"]!!)
    }

    @Test
    fun `should delete director successfully`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 2
        every { directorRepository.delete(testDirector) } returns Unit

        directorService.deleteDirector(1L)

        verify { directorRepository.delete(testDirector) }
        verify { auditService.logDirectorDeletion(1L, 1L, "John Doe") }
    }

    @Test
    fun `should throw validation exception when deleting last active director`() {
        every { directorRepository.findById(1L) } returns Optional.of(testDirector)
        every { directorRepository.countActiveDirectorsByCompanyId(1L) } returns 1

        val exception = assertThrows<ValidationException> {
            directorService.deleteDirector(1L)
        }

        assertEquals("deletion", exception.field)
        assertTrue(exception.message!!.contains("at least one director"))
    }
}
