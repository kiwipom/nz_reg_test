package nz.govt.companiesoffice.register.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.entity.Shareholder
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.ShareholderRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShareholderServiceTest {

    private val shareholderRepository = mockk<ShareholderRepository>()
    private val auditService = mockk<AuditService>(relaxed = true)
    private val shareholderNotificationService = mockk<ShareholderNotificationService>(relaxed = true)
    private val shareholderService =
        ShareholderService(shareholderRepository, auditService, shareholderNotificationService)

    private lateinit var testCompany: Company
    private lateinit var testShareholder: Shareholder

    @BeforeEach
    fun setUp() {
        testCompany = Company(
            id = 1L,
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.now(),
        )

        testShareholder = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "John Smith",
            addressLine1 = "123 Main St",
            addressLine2 = "Unit 5",
            city = "Auckland",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            isIndividual = true,
        )
    }

    @Test
    fun `should create shareholder successfully`() {
        every { shareholderRepository.existsByCompanyIdAndFullNameIgnoreCase(1L, "John Smith") } returns false
        every { shareholderRepository.save(any<Shareholder>()) } returns testShareholder

        val result = shareholderService.createShareholder(testShareholder)

        assertEquals(testShareholder, result)
        verify { shareholderRepository.save(testShareholder) }
        verify { auditService.logShareholderCreation(1L, 1L, "John Smith") }
    }

    @Test
    fun `should throw validation exception when shareholder already exists`() {
        every { shareholderRepository.existsByCompanyIdAndFullNameIgnoreCase(1L, "John Smith") } returns true

        val exception = assertThrows<ValidationException> {
            shareholderService.createShareholder(testShareholder)
        }

        assertEquals("fullName", exception.field)
        assertTrue(exception.message!!.contains("already exists"))
    }

    @Test
    fun `should update shareholder successfully`() {
        val updatedData = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "Jane Smith",
            addressLine1 = "456 Oak Ave",
            addressLine2 = null,
            city = "Wellington",
            region = "Wellington",
            postcode = "6011",
            country = "NZ",
            isIndividual = true,
        )

        every { shareholderRepository.findById(1L) } returns Optional.of(testShareholder)
        every { shareholderRepository.save(any<Shareholder>()) } returns testShareholder.apply {
            fullName = "Jane Smith"
            addressLine1 = "456 Oak Ave"
            addressLine2 = null
            city = "Wellington"
            region = "Wellington"
            postcode = "6011"
        }

        val result = shareholderService.updateShareholder(1L, updatedData)

        assertEquals("Jane Smith", result.fullName)
        assertEquals("456 Oak Ave", result.addressLine1)
        assertEquals("Wellington", result.city)
        verify { auditService.logShareholderUpdate(1L, 1L, "Jane Smith") }
    }

    @Test
    fun `should delete shareholder successfully`() {
        every { shareholderRepository.findById(1L) } returns Optional.of(testShareholder)
        every { shareholderRepository.delete(testShareholder) } returns Unit

        shareholderService.deleteShareholder(1L)

        verify { shareholderRepository.delete(testShareholder) }
        verify { auditService.logShareholderDeletion(1L, 1L, "John Smith") }
    }

    @Test
    fun `should get shareholder by id successfully`() {
        every { shareholderRepository.findById(1L) } returns Optional.of(testShareholder)

        val result = shareholderService.getShareholderById(1L)

        assertEquals(testShareholder, result)
    }

    @Test
    fun `should throw resource not found exception when shareholder does not exist`() {
        every { shareholderRepository.findById(1L) } returns Optional.empty()

        val exception = assertThrows<ResourceNotFoundException> {
            shareholderService.getShareholderById(1L)
        }

        assertTrue(exception.message!!.contains("Shareholder not found"))
    }

    @Test
    fun `should get shareholders by company`() {
        val shareholders = listOf(testShareholder)
        every { shareholderRepository.findByCompanyId(1L) } returns shareholders

        val result = shareholderService.getShareholdersByCompany(1L)

        assertEquals(shareholders, result)
    }

    @Test
    fun `should get individual shareholders by company`() {
        val individualShareholders = listOf(testShareholder)
        every { shareholderRepository.findByCompanyIdAndIsIndividualTrue(1L) } returns individualShareholders

        val result = shareholderService.getIndividualShareholdersByCompany(1L)

        assertEquals(individualShareholders, result)
    }

    @Test
    fun `should get corporate shareholders by company`() {
        val corporateShareholder = Shareholder(
            id = 2L,
            company = testCompany,
            fullName = "ABC Corporation Ltd",
            addressLine1 = "789 Business St",
            city = "Auckland",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            isIndividual = false,
        )
        val corporateShareholders = listOf(corporateShareholder)
        every { shareholderRepository.findByCompanyIdAndIsIndividualFalse(1L) } returns corporateShareholders

        val result = shareholderService.getCorporateShareholdersByCompany(1L)

        assertEquals(corporateShareholders, result)
    }

    @Test
    fun `should get shareholders by location`() {
        val shareholders = listOf(testShareholder)
        every {
            shareholderRepository.findByCompanyIdAndCityIgnoreCaseAndCountryIgnoreCase(
                1L,
                "Auckland",
                "NZ",
            )
        } returns shareholders

        val result = shareholderService.getShareholdersByLocation(1L, "Auckland", "NZ")

        assertEquals(shareholders, result)
    }

    @Test
    fun `should get shareholders by country`() {
        val shareholders = listOf(testShareholder)
        every {
            shareholderRepository.findByCompanyIdAndCountryIgnoreCase(1L, "NZ")
        } returns shareholders

        val result = shareholderService.getShareholdersByCountry(1L, "NZ")

        assertEquals(shareholders, result)
    }

    @Test
    fun `should get shareholders by region`() {
        val shareholders = listOf(testShareholder)
        every {
            shareholderRepository.findByCompanyIdAndRegionIgnoreCase(1L, "Auckland")
        } returns shareholders

        val result = shareholderService.getShareholdersByRegion(1L, "Auckland")

        assertEquals(shareholders, result)
    }

    @Test
    fun `should get shareholders by postcode`() {
        val shareholders = listOf(testShareholder)
        every {
            shareholderRepository.findByCompanyIdAndPostcode(1L, "1010")
        } returns shareholders

        val result = shareholderService.getShareholdersByPostcode(1L, "1010")

        assertEquals(shareholders, result)
    }

    @Test
    fun `should search shareholders successfully`() {
        val searchTerm = "John"
        val shareholders = listOf(testShareholder)
        every { shareholderRepository.searchShareholders(searchTerm) } returns shareholders

        val result = shareholderService.searchShareholders(searchTerm)

        assertEquals(shareholders, result)
        verify { auditService.logShareholderSearch(searchTerm, 1) }
    }

    @Test
    fun `should search shareholders by address`() {
        val addressTerm = "Main St"
        val shareholders = listOf(testShareholder)
        every {
            shareholderRepository.findByAddressContaining(1L, addressTerm)
        } returns shareholders

        val result = shareholderService.searchShareholdersByAddress(1L, addressTerm)

        assertEquals(shareholders, result)
    }

    @Test
    fun `should get shareholder statistics successfully`() {
        every { shareholderRepository.countShareholdersByCompanyId(1L) } returns 10L
        every { shareholderRepository.countShareholdersByType(1L, true) } returns 7L
        every { shareholderRepository.countShareholdersByType(1L, false) } returns 3L

        val result = shareholderService.getShareholderStatistics(1L)

        assertEquals(10L, result["totalShareholders"])
        assertEquals(7L, result["individualShareholders"])
        assertEquals(3L, result["corporateShareholders"])
    }

    @Test
    fun `should validate shareholder data with valid data`() {
        val validShareholder = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "John Smith",
            addressLine1 = "123 Main St",
            city = "Auckland",
            country = "NZ",
            isIndividual = true,
        )

        val result = shareholderService.validateShareholderData(validShareholder)

        assertTrue(result["hasValidName"]!!)
        assertTrue(result["hasValidAddress"]!!)
        assertTrue(result["hasValidCountry"]!!)
    }

    @Test
    fun `should validate shareholder data with invalid data`() {
        val invalidShareholder = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "",
            addressLine1 = "",
            city = "",
            country = "",
            isIndividual = true,
        )

        val result = shareholderService.validateShareholderData(invalidShareholder)

        assertFalse(result["hasValidName"]!!)
        assertFalse(result["hasValidAddress"]!!)
        assertFalse(result["hasValidCountry"]!!)
    }

    @Test
    fun `should handle empty search results`() {
        val searchTerm = "NonExistent"
        every { shareholderRepository.searchShareholders(searchTerm) } returns emptyList()

        val result = shareholderService.searchShareholders(searchTerm)

        assertTrue(result.isEmpty())
        verify { auditService.logShareholderSearch(searchTerm, 0) }
    }

    @Test
    fun `should handle null address line 2 in update`() {
        val updatedData = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "John Smith Updated",
            addressLine1 = "123 Updated St",
            addressLine2 = null,
            city = "Auckland",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            isIndividual = true,
        )

        every { shareholderRepository.findById(1L) } returns Optional.of(testShareholder)
        every { shareholderRepository.save(any<Shareholder>()) } returns testShareholder.apply {
            fullName = "John Smith Updated"
            addressLine1 = "123 Updated St"
            addressLine2 = null
        }

        val result = shareholderService.updateShareholder(1L, updatedData)

        assertEquals("John Smith Updated", result.fullName)
        assertEquals("123 Updated St", result.addressLine1)
        assertEquals(null, result.addressLine2)
    }

    @Test
    fun `should handle shareholder type conversion in update`() {
        val updatedData = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "Smith Corporation Ltd",
            addressLine1 = "123 Business St",
            city = "Auckland",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            isIndividual = false,
        )

        every { shareholderRepository.findById(1L) } returns Optional.of(testShareholder)
        every { shareholderRepository.save(any<Shareholder>()) } returns testShareholder.apply {
            fullName = "Smith Corporation Ltd"
            isIndividual = false
        }

        val result = shareholderService.updateShareholder(1L, updatedData)

        assertEquals("Smith Corporation Ltd", result.fullName)
        assertFalse(result.isIndividual)
    }

    @Test
    fun `should validate partial address data correctly`() {
        val partialAddressShareholder = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "John Smith",
            addressLine1 = "123 Main St",
            addressLine2 = null,
            city = "",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            isIndividual = true,
        )

        val result = shareholderService.validateShareholderData(partialAddressShareholder)

        assertTrue(result["hasValidName"]!!)
        assertFalse(result["hasValidAddress"]!!) // city is blank
        assertTrue(result["hasValidCountry"]!!)
    }

    @Test
    fun `should handle international shareholders`() {
        val internationalShareholder = Shareholder(
            id = 2L,
            company = testCompany,
            fullName = "International Corp",
            addressLine1 = "456 Foreign St",
            city = "London",
            region = "England",
            postcode = "SW1A 1AA",
            country = "UK",
            isIndividual = false,
        )

        every { shareholderRepository.existsByCompanyIdAndFullNameIgnoreCase(1L, "International Corp") } returns false
        every { shareholderRepository.save(any<Shareholder>()) } returns internationalShareholder

        val result = shareholderService.createShareholder(internationalShareholder)

        assertEquals("UK", result.country)
        assertEquals("London", result.city)
        assertEquals("SW1A 1AA", result.postcode)
        assertFalse(result.isIndividual)
    }

    @Test
    fun `should handle case insensitive name search`() {
        every {
            shareholderRepository.existsByCompanyIdAndFullNameIgnoreCase(1L, "JOHN SMITH")
        } returns true

        val shareholderWithUppercaseName = Shareholder(
            id = 2L,
            company = testCompany,
            fullName = "JOHN SMITH",
            addressLine1 = "123 Main St",
            city = "Auckland",
            country = "NZ",
            isIndividual = true,
        )

        val exception = assertThrows<ValidationException> {
            shareholderService.createShareholder(shareholderWithUppercaseName)
        }

        assertEquals("fullName", exception.field)
        assertTrue(exception.message!!.contains("already exists"))
    }

    @Test
    fun `should handle statistics when no shareholders exist`() {
        every { shareholderRepository.countShareholdersByCompanyId(1L) } returns 0L
        every { shareholderRepository.countShareholdersByType(1L, true) } returns 0L
        every { shareholderRepository.countShareholdersByType(1L, false) } returns 0L

        val result = shareholderService.getShareholderStatistics(1L)

        assertEquals(0L, result["totalShareholders"])
        assertEquals(0L, result["individualShareholders"])
        assertEquals(0L, result["corporateShareholders"])
    }
}
