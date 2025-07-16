package nz.govt.companiesoffice.register.service

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.CompanyRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
class CompanyServiceTest {

    private val companyRepository: CompanyRepository = mockk()
    private val auditService: AuditService = mockk()
    private lateinit var companyService: CompanyService

    private lateinit var testCompany: Company
    private lateinit var testCompanyList: List<Company>

    @BeforeEach
    fun setUp() {
        companyService = CompanyService(companyRepository, auditService)

        testCompany = Company(
            id = 1L,
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.of(2020, 1, 1),
            nzbn = "9429000000000",
            status = "ACTIVE",
        )

        testCompanyList = listOf(
            testCompany,
            Company(
                id = 2L,
                companyNumber = "87654321",
                companyName = "Another Company Ltd",
                companyType = CompanyType.LTD,
                incorporationDate = LocalDate.of(2021, 1, 1),
                nzbn = "9429000000001",
                status = "ACTIVE",
            ),
        )
    }

    @Nested
    @DisplayName("Create Company Tests")
    inner class CreateCompanyTests {

        @Test
        @DisplayName("Should create company successfully with valid data")
        fun `createCompany should create company successfully with valid data`() {
            // Given
            val newCompany = testCompany.copy(id = null)
            val savedCompany = testCompany.copy()

            every { companyRepository.existsByCompanyNameIgnoreCase(newCompany.companyName) } returns false
            every { companyRepository.existsByCompanyNumber(newCompany.companyNumber) } returns false
            every { companyRepository.save(newCompany) } returns savedCompany
            justRun { auditService.logCompanyCreation(savedCompany.id!!, savedCompany.companyName) }

            // When
            val result = companyService.createCompany(newCompany)

            // Then
            assertEquals(savedCompany, result)
            verify { companyRepository.existsByCompanyNameIgnoreCase(newCompany.companyName) }
            verify { companyRepository.existsByCompanyNumber(newCompany.companyNumber) }
            verify { companyRepository.save(newCompany) }
            verify { auditService.logCompanyCreation(savedCompany.id!!, savedCompany.companyName) }
        }

        @Test
        @DisplayName("Should throw ValidationException when company name already exists")
        fun `createCompany should throw ValidationException when company name already exists`() {
            // Given
            val newCompany = testCompany.copy(id = null)
            every { companyRepository.existsByCompanyNameIgnoreCase(newCompany.companyName) } returns true

            // When & Then
            val exception = assertThrows<ValidationException> {
                companyService.createCompany(newCompany)
            }

            assertEquals("companyName", exception.field)
            assertEquals("Company name already exists", exception.message)
            verify { companyRepository.existsByCompanyNameIgnoreCase(newCompany.companyName) }
            verify(exactly = 0) { companyRepository.save(any()) }
        }

        @Test
        @DisplayName("Should throw ValidationException when company number already exists")
        fun `createCompany should throw ValidationException when company number already exists`() {
            // Given
            val newCompany = testCompany.copy(id = null)
            every { companyRepository.existsByCompanyNameIgnoreCase(newCompany.companyName) } returns false
            every { companyRepository.existsByCompanyNumber(newCompany.companyNumber) } returns true

            // When & Then
            val exception = assertThrows<ValidationException> {
                companyService.createCompany(newCompany)
            }

            assertEquals("companyNumber", exception.field)
            assertEquals("Company number already exists", exception.message)
            verify { companyRepository.existsByCompanyNameIgnoreCase(newCompany.companyName) }
            verify { companyRepository.existsByCompanyNumber(newCompany.companyNumber) }
            verify(exactly = 0) { companyRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("Get Company Tests")
    inner class GetCompanyTests {

        @Test
        @DisplayName("Should get company by ID successfully")
        fun `getCompanyById should return company when found`() {
            // Given
            val companyId = 1L
            every { companyRepository.findById(companyId) } returns Optional.of(testCompany)
            justRun { auditService.logCompanyAccess(companyId) }

            // When
            val result = companyService.getCompanyById(companyId)

            // Then
            assertEquals(testCompany, result)
            verify { companyRepository.findById(companyId) }
            verify { auditService.logCompanyAccess(companyId) }
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when company ID not found")
        fun `getCompanyById should throw ResourceNotFoundException when not found`() {
            // Given
            val companyId = 999L
            every { companyRepository.findById(companyId) } returns Optional.empty()

            // When & Then
            val exception = assertThrows<ResourceNotFoundException> {
                companyService.getCompanyById(companyId)
            }

            assertEquals("company", exception.resourceType)
            assertTrue(exception.message!!.contains("Company not found with id: $companyId"))
            verify { companyRepository.findById(companyId) }
            verify(exactly = 0) { auditService.logCompanyAccess(any()) }
        }

        @Test
        @DisplayName("Should get company by number successfully")
        fun `getCompanyByNumber should return company when found`() {
            // Given
            val companyNumber = "12345678"
            every { companyRepository.findByCompanyNumber(companyNumber) } returns testCompany

            // When
            val result = companyService.getCompanyByNumber(companyNumber)

            // Then
            assertEquals(testCompany, result)
            verify { companyRepository.findByCompanyNumber(companyNumber) }
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when company number not found")
        fun `getCompanyByNumber should throw ResourceNotFoundException when not found`() {
            // Given
            val companyNumber = "99999999"
            every { companyRepository.findByCompanyNumber(companyNumber) } returns null

            // When & Then
            val exception = assertThrows<ResourceNotFoundException> {
                companyService.getCompanyByNumber(companyNumber)
            }

            assertEquals("company", exception.resourceType)
            assertTrue(exception.message!!.contains("Company not found with number: $companyNumber"))
            verify { companyRepository.findByCompanyNumber(companyNumber) }
        }
    }

    @Nested
    @DisplayName("Search Company Tests")
    inner class SearchCompanyTests {

        @Test
        @DisplayName("Should search companies successfully")
        fun `searchCompanies should return companies and log search`() {
            // Given
            val query = "Test Company"
            every { companyRepository.searchCompanies(query) } returns testCompanyList
            justRun { auditService.logCompanySearch(query, testCompanyList.size) }

            // When
            val result = companyService.searchCompanies(query)

            // Then
            assertEquals(testCompanyList, result)
            verify { companyRepository.searchCompanies(query) }
            verify { auditService.logCompanySearch(query, testCompanyList.size) }
        }

        @Test
        @DisplayName("Should return empty list when no companies match search")
        fun `searchCompanies should return empty list when no matches`() {
            // Given
            val query = "NonExistent Company"
            val emptyList = emptyList<Company>()
            every { companyRepository.searchCompanies(query) } returns emptyList
            justRun { auditService.logCompanySearch(query, 0) }

            // When
            val result = companyService.searchCompanies(query)

            // Then
            assertEquals(emptyList, result)
            verify { companyRepository.searchCompanies(query) }
            verify { auditService.logCompanySearch(query, 0) }
        }

        @Test
        @DisplayName("Should get companies by type successfully")
        fun `getCompaniesByType should return companies of specified type`() {
            // Given
            val companyType = CompanyType.LTD
            val limitedCompanies = testCompanyList.filter { it.companyType == CompanyType.LTD }
            every { companyRepository.findByCompanyType(companyType) } returns limitedCompanies

            // When
            val result = companyService.getCompaniesByType(companyType)

            // Then
            assertEquals(limitedCompanies, result)
            verify { companyRepository.findByCompanyType(companyType) }
        }

        @Test
        @DisplayName("Should get active companies successfully")
        fun `getActiveCompanies should return active companies`() {
            // Given
            val activeCompanies = testCompanyList.filter { it.status == "ACTIVE" }
            every { companyRepository.findByStatus("ACTIVE") } returns activeCompanies

            // When
            val result = companyService.getActiveCompanies()

            // Then
            assertEquals(activeCompanies, result)
            verify { companyRepository.findByStatus("ACTIVE") }
        }
    }

    @Nested
    @DisplayName("Update Company Tests")
    inner class UpdateCompanyTests {

        @Test
        @DisplayName("Should update company successfully")
        fun `updateCompany should update company successfully`() {
            // Given
            val companyId = 1L
            val existingCompany = testCompany.copy()
            val updatedData = testCompany.copy(
                companyName = "Updated Company Name",
                nzbn = "9429000000002",
                status = "INACTIVE",
            )
            val savedCompany = existingCompany.copy(
                companyName = updatedData.companyName,
                nzbn = updatedData.nzbn,
                status = updatedData.status,
            )

            every { companyRepository.findById(companyId) } returns Optional.of(existingCompany)
            every { companyRepository.save(existingCompany) } returns savedCompany
            justRun { auditService.logCompanyAccess(companyId) }

            // When
            val result = companyService.updateCompany(companyId, updatedData)

            // Then
            assertEquals(savedCompany, result)
            assertEquals("Updated Company Name", result.companyName)
            assertEquals("9429000000002", result.nzbn)
            assertEquals("INACTIVE", result.status)
            verify { companyRepository.findById(companyId) }
            verify { companyRepository.save(existingCompany) }
            verify { auditService.logCompanyAccess(companyId) }
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent company")
        fun `updateCompany should throw ResourceNotFoundException when company not found`() {
            // Given
            val companyId = 999L
            val updatedData = testCompany.copy(companyName = "Updated Name")
            every { companyRepository.findById(companyId) } returns Optional.empty()

            // When & Then
            val exception = assertThrows<ResourceNotFoundException> {
                companyService.updateCompany(companyId, updatedData)
            }

            assertEquals("company", exception.resourceType)
            assertTrue(exception.message!!.contains("Company not found with id: $companyId"))
            verify { companyRepository.findById(companyId) }
            verify(exactly = 0) { companyRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("Delete Company Tests")
    inner class DeleteCompanyTests {

        @Test
        @DisplayName("Should delete company successfully")
        fun `deleteCompany should delete company successfully`() {
            // Given
            val companyId = 1L
            every { companyRepository.findById(companyId) } returns Optional.of(testCompany)
            justRun { companyRepository.delete(testCompany) }
            justRun { auditService.logCompanyAccess(companyId) }

            // When
            companyService.deleteCompany(companyId)

            // Then
            verify { companyRepository.findById(companyId) }
            verify { companyRepository.delete(testCompany) }
            verify { auditService.logCompanyAccess(companyId) }
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent company")
        fun `deleteCompany should throw ResourceNotFoundException when company not found`() {
            // Given
            val companyId = 999L
            every { companyRepository.findById(companyId) } returns Optional.empty()

            // When & Then
            val exception = assertThrows<ResourceNotFoundException> {
                companyService.deleteCompany(companyId)
            }

            assertEquals("company", exception.resourceType)
            assertTrue(exception.message!!.contains("Company not found with id: $companyId"))
            verify { companyRepository.findById(companyId) }
            verify(exactly = 0) { companyRepository.delete(any()) }
        }
    }

    @Nested
    @DisplayName("Availability Check Tests")
    inner class AvailabilityCheckTests {

        @Test
        @DisplayName("Should return true when company name is available")
        fun `isCompanyNameAvailable should return true when name is available`() {
            // Given
            val companyName = "Available Company Name"
            every { companyRepository.existsByCompanyNameIgnoreCase(companyName) } returns false

            // When
            val result = companyService.isCompanyNameAvailable(companyName)

            // Then
            assertTrue(result)
            verify { companyRepository.existsByCompanyNameIgnoreCase(companyName) }
        }

        @Test
        @DisplayName("Should return false when company name is not available")
        fun `isCompanyNameAvailable should return false when name is not available`() {
            // Given
            val companyName = "Existing Company Name"
            every { companyRepository.existsByCompanyNameIgnoreCase(companyName) } returns true

            // When
            val result = companyService.isCompanyNameAvailable(companyName)

            // Then
            assertFalse(result)
            verify { companyRepository.existsByCompanyNameIgnoreCase(companyName) }
        }

        @Test
        @DisplayName("Should return true when company number is available")
        fun `isCompanyNumberAvailable should return true when number is available`() {
            // Given
            val companyNumber = "99999999"
            every { companyRepository.existsByCompanyNumber(companyNumber) } returns false

            // When
            val result = companyService.isCompanyNumberAvailable(companyNumber)

            // Then
            assertTrue(result)
            verify { companyRepository.existsByCompanyNumber(companyNumber) }
        }

        @Test
        @DisplayName("Should return false when company number is not available")
        fun `isCompanyNumberAvailable should return false when number is not available`() {
            // Given
            val companyNumber = "12345678"
            every { companyRepository.existsByCompanyNumber(companyNumber) } returns true

            // When
            val result = companyService.isCompanyNumberAvailable(companyNumber)

            // Then
            assertFalse(result)
            verify { companyRepository.existsByCompanyNumber(companyNumber) }
        }
    }
}
