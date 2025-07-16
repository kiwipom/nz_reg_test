package nz.govt.companiesoffice.register.integration

import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.CompanyRepository
import nz.govt.companiesoffice.register.service.CompanyService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class CompanyServiceIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:15")
            .withDatabaseName("companies_register_test")
            .withUsername("test")
            .withPassword("test")
    }

    @Autowired
    private lateinit var companyService: CompanyService

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    private lateinit var testCompany: Company

    @BeforeEach
    fun setUp() {
        companyRepository.deleteAll()
        
        testCompany = Company(
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.of(2020, 1, 1),
            nzbn = "9429000000000",
            status = "ACTIVE"
        )
    }

    @Nested
    @DisplayName("Company Creation Integration Tests")
    inner class CompanyCreationTests {

        @Test
        @DisplayName("Should create and persist company successfully")
        fun `createCompany should persist company to database`() {
            // When
            val createdCompany = companyService.createCompany(testCompany)

            // Then
            assertNotNull(createdCompany.id)
            assertEquals(testCompany.companyName, createdCompany.companyName)
            assertEquals(testCompany.companyNumber, createdCompany.companyNumber)
            assertEquals(testCompany.companyType, createdCompany.companyType)
            assertEquals(testCompany.incorporationDate, createdCompany.incorporationDate)
            assertEquals(testCompany.nzbn, createdCompany.nzbn)
            assertEquals(testCompany.status, createdCompany.status)

            // Verify persistence
            val savedCompany = companyRepository.findById(createdCompany.id!!)
            assertTrue(savedCompany.isPresent)
            assertEquals(createdCompany.companyName, savedCompany.get().companyName)
        }

        @Test
        @DisplayName("Should validate unique company number")
        fun `createCompany should throw exception for duplicate company number`() {
            // Given
            companyRepository.save(testCompany)
            val duplicateCompany = Company(
                companyNumber = testCompany.companyNumber,
                companyName = "Different Name",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )

            // When & Then
            assertThrows<ValidationException> {
                companyService.createCompany(duplicateCompany)
            }
        }

        @Test
        @DisplayName("Should validate unique company name")
        fun `createCompany should throw exception for duplicate company name`() {
            // Given
            companyRepository.save(testCompany)
            val duplicateCompany = Company(
                companyNumber = "87654321",
                companyName = testCompany.companyName,
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )

            // When & Then
            assertThrows<ValidationException> {
                companyService.createCompany(duplicateCompany)
            }
        }

        @Test
        @DisplayName("Should validate unique NZBN")
        fun `createCompany should throw exception for duplicate NZBN`() {
            // Given
            companyRepository.save(testCompany)
            val duplicateCompany = Company(
                companyNumber = "87654321",
                companyName = "Different Name",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )

            // When & Then
            assertThrows<ValidationException> {
                companyService.createCompany(duplicateCompany)
            }
        }

        @Test
        @DisplayName("Should handle multiple companies with different details")
        fun `createCompany should allow multiple companies with different details`() {
            // Given
            val company1 = testCompany
            val company2 = Company(
                companyNumber = "87654321",
                companyName = "Another Company Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )

            // When
            val createdCompany1 = companyService.createCompany(company1)
            val createdCompany2 = companyService.createCompany(company2)

            // Then
            assertNotNull(createdCompany1.id)
            assertNotNull(createdCompany2.id)
            assertEquals(2, companyRepository.count())
        }
    }

    @Nested
    @DisplayName("Company Retrieval Integration Tests")
    inner class CompanyRetrievalTests {

        @Test
        @DisplayName("Should retrieve company by ID")
        fun `getCompanyById should return company from database`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)

            // When
            val retrievedCompany = companyService.getCompanyById(savedCompany.id!!)

            // Then
            assertEquals(savedCompany.id, retrievedCompany.id)
            assertEquals(savedCompany.companyName, retrievedCompany.companyName)
            assertEquals(savedCompany.companyNumber, retrievedCompany.companyNumber)
        }

        @Test
        @DisplayName("Should throw exception for non-existent company ID")
        fun `getCompanyById should throw exception for non-existent ID`() {
            // When & Then
            assertThrows<ResourceNotFoundException> {
                companyService.getCompanyById(999999L)
            }
        }

        @Test
        @DisplayName("Should retrieve company by number")
        fun `getCompanyByNumber should return company from database`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)

            // When
            val retrievedCompany = companyService.getCompanyByNumber(savedCompany.companyNumber)

            // Then
            assertEquals(savedCompany.id, retrievedCompany.id)
            assertEquals(savedCompany.companyName, retrievedCompany.companyName)
            assertEquals(savedCompany.companyNumber, retrievedCompany.companyNumber)
        }

        @Test
        @DisplayName("Should throw exception for non-existent company number")
        fun `getCompanyByNumber should throw exception for non-existent number`() {
            // When & Then
            assertThrows<ResourceNotFoundException> {
                companyService.getCompanyByNumber("99999999")
            }
        }

        @Test
        @DisplayName("Should retrieve active companies only")
        fun `getActiveCompanies should return only active companies`() {
            // Given
            val activeCompany1 = Company(
                companyNumber = "11111111",
                companyName = testCompany.companyName,
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val activeCompany2 = Company(
                companyNumber = "22222222",
                companyName = "Active Company 2",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )
            val inactiveCompany = Company(
                companyNumber = "33333333",
                companyName = "Inactive Company",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000002",
                status = "INACTIVE",
            )

            companyRepository.save(activeCompany1)
            companyRepository.save(activeCompany2)
            companyRepository.save(inactiveCompany)

            // When
            val activeCompanies = companyService.getActiveCompanies()

            // Then
            assertEquals(2, activeCompanies.size)
            assertTrue(activeCompanies.all { it.status == "ACTIVE" })
            assertTrue(activeCompanies.any { it.companyNumber == "11111111" })
            assertTrue(activeCompanies.any { it.companyNumber == "22222222" })
            assertFalse(activeCompanies.any { it.companyNumber == "33333333" })
        }
    }

    @Nested
    @DisplayName("Company Update Integration Tests")
    inner class CompanyUpdateTests {

        @Test
        @DisplayName("Should update company successfully")
        fun `updateCompany should persist changes to database`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)
            val updateData = Company(
                id = savedCompany.id,
                companyNumber = savedCompany.companyNumber,
                companyName = "Updated Company Ltd",
                companyType = savedCompany.companyType,
                incorporationDate = savedCompany.incorporationDate,
                nzbn = savedCompany.nzbn,
                status = "SUSPENDED",
            )

            // When
            val updatedCompany = companyService.updateCompany(savedCompany.id!!, updateData)

            // Then
            assertEquals(savedCompany.id, updatedCompany.id)
            assertEquals("Updated Company Ltd", updatedCompany.companyName)
            assertEquals("SUSPENDED", updatedCompany.status)
            assertEquals(savedCompany.companyNumber, updatedCompany.companyNumber) // Should not change

            // Verify persistence
            val dbCompany = companyRepository.findById(savedCompany.id!!)
            assertTrue(dbCompany.isPresent)
            assertEquals("Updated Company Ltd", dbCompany.get().companyName)
            assertEquals("SUSPENDED", dbCompany.get().status)
        }

        @Test
        @DisplayName("Should throw exception for non-existent company update")
        fun `updateCompany should throw exception for non-existent ID`() {
            // Given
            val updateData = Company(
                companyNumber = testCompany.companyNumber,
                companyName = "Updated Company",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )

            // When & Then
            assertThrows<ResourceNotFoundException> {
                companyService.updateCompany(999999L, updateData)
            }
        }

        @Test
        @DisplayName("Should validate uniqueness constraints on update")
        fun `updateCompany should validate unique constraints`() {
            // Given
            val company1 = Company(
                companyNumber = "11111111",
                companyName = testCompany.companyName,
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "22222222",
                companyName = "Company 2 Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )
            val saved1 = companyRepository.save(company1)
            val saved2 = companyRepository.save(company2)

            // Try to update company2 with company1's name
            val updateData = Company(
                id = saved2.id,
                companyNumber = saved2.companyNumber,
                companyName = saved1.companyName,
                companyType = saved2.companyType,
                incorporationDate = saved2.incorporationDate,
                nzbn = saved2.nzbn,
                status = saved2.status,
            )

            // When & Then
            assertThrows<ValidationException> {
                companyService.updateCompany(saved2.id!!, updateData)
            }
        }
    }

    @Nested
    @DisplayName("Company Deletion Integration Tests")
    inner class CompanyDeletionTests {

        @Test
        @DisplayName("Should delete company successfully")
        fun `deleteCompany should remove company from database`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)

            // When
            companyService.deleteCompany(savedCompany.id!!)

            // Then
            val deletedCompany = companyRepository.findById(savedCompany.id!!)
            assertFalse(deletedCompany.isPresent)
        }

        @Test
        @DisplayName("Should throw exception for non-existent company deletion")
        fun `deleteCompany should throw exception for non-existent ID`() {
            // When & Then
            assertThrows<ResourceNotFoundException> {
                companyService.deleteCompany(999999L)
            }
        }
    }

    @Nested
    @DisplayName("Company Search Integration Tests")
    inner class CompanySearchTests {

        @Test
        @DisplayName("Should search companies by name")
        fun `searchCompanies should find companies by name`() {
            // Given
            val company1 = Company(
                companyNumber = "11111111",
                companyName = "Alpha Company Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "22222222",
                companyName = "Beta Company Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )
            val company3 = Company(
                companyNumber = "33333333",
                companyName = "Gamma Corporation",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000002",
                status = testCompany.status,
            )

            companyRepository.save(company1)
            companyRepository.save(company2)
            companyRepository.save(company3)

            // When
            val results = companyService.searchCompanies("Company")

            // Then
            assertEquals(2, results.size)
            assertTrue(results.any { it.companyName == "Alpha Company Ltd" })
            assertTrue(results.any { it.companyName == "Beta Company Ltd" })
            assertFalse(results.any { it.companyName == "Gamma Corporation" })
        }

        @Test
        @DisplayName("Should search companies case-insensitively")
        fun `searchCompanies should be case-insensitive`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)

            // When
            val results1 = companyService.searchCompanies("test")
            val results2 = companyService.searchCompanies("TEST")
            val results3 = companyService.searchCompanies("Test")

            // Then
            assertEquals(1, results1.size)
            assertEquals(1, results2.size)
            assertEquals(1, results3.size)
            assertEquals(savedCompany.companyName, results1[0].companyName)
            assertEquals(savedCompany.companyName, results2[0].companyName)
            assertEquals(savedCompany.companyName, results3[0].companyName)
        }

        @Test
        @DisplayName("Should return empty results for non-matching search")
        fun `searchCompanies should return empty list for non-matching query`() {
            // Given
            companyRepository.save(testCompany)

            // When
            val results = companyService.searchCompanies("NonExistent")

            // Then
            assertTrue(results.isEmpty())
        }

        @Test
        @DisplayName("Should handle partial matches")
        fun `searchCompanies should handle partial matches`() {
            // Given
            val company1 = Company(
                companyNumber = "11111111",
                companyName = "Software Development Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "22222222",
                companyName = "Hardware Solutions Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )
            val company3 = Company(
                companyNumber = "33333333",
                companyName = "Marketing Services Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000002",
                status = testCompany.status,
            )

            companyRepository.save(company1)
            companyRepository.save(company2)
            companyRepository.save(company3)

            // When
            val results = companyService.searchCompanies("ware")

            // Then
            assertEquals(2, results.size)
            assertTrue(results.any { it.companyName == "Software Development Ltd" })
            assertTrue(results.any { it.companyName == "Hardware Solutions Ltd" })
        }
    }

    @Nested
    @DisplayName("Company Availability Check Integration Tests")
    inner class CompanyAvailabilityTests {

        @Test
        @DisplayName("Should check company name availability")
        fun `isCompanyNameAvailable should check database accurately`() {
            // Given
            companyRepository.save(testCompany)

            // When & Then
            assertFalse(companyService.isCompanyNameAvailable("Test Company Ltd"))
            assertTrue(companyService.isCompanyNameAvailable("Available Company Ltd"))
        }

        @Test
        @DisplayName("Should check company name availability case-insensitively")
        fun `isCompanyNameAvailable should be case-insensitive`() {
            // Given
            companyRepository.save(testCompany)

            // When & Then
            assertFalse(companyService.isCompanyNameAvailable("test company ltd"))
            assertFalse(companyService.isCompanyNameAvailable("TEST COMPANY LTD"))
            assertFalse(companyService.isCompanyNameAvailable("Test Company Ltd"))
        }

        @Test
        @DisplayName("Should check company number availability")
        fun `isCompanyNumberAvailable should check database accurately`() {
            // Given
            companyRepository.save(testCompany)

            // When & Then
            assertFalse(companyService.isCompanyNumberAvailable("12345678"))
            assertTrue(companyService.isCompanyNumberAvailable("87654321"))
        }

        @Test
        @DisplayName("Should handle availability checks with empty database")
        fun `availability checks should return true for empty database`() {
            // When & Then
            assertTrue(companyService.isCompanyNameAvailable("Any Company Ltd"))
            assertTrue(companyService.isCompanyNumberAvailable("12345678"))
        }
    }

    @Nested
    @DisplayName("Transaction Integration Tests")
    inner class TransactionTests {

        @Test
        @DisplayName("Should handle transaction rollback on error")
        fun `should rollback transaction on validation error`() {
            // Given
            val validCompany = Company(
                companyNumber = "11111111",
                companyName = testCompany.companyName,
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            companyRepository.save(validCompany)

            // When & Then
            assertThrows<ValidationException> {
                companyService.createCompany(testCompany) // Duplicate company number
            }

            // Verify original company still exists and no new company was created
            assertEquals(1, companyRepository.count())
            val existingCompany = companyRepository.findByCompanyNumber("11111111")
            assertNotNull(existingCompany)
        }

        @Test
        @DisplayName("Should handle multiple operations in transaction")
        fun `should handle multiple operations within transaction`() {
            // Given
            val company1 = Company(
                companyNumber = "11111111",
                companyName = testCompany.companyName,
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "22222222",
                companyName = "Company 2 Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )

            // When
            val created1 = companyService.createCompany(company1)
            val created2 = companyService.createCompany(company2)
            val retrieved1 = companyService.getCompanyById(created1.id!!)
            val retrieved2 = companyService.getCompanyById(created2.id!!)

            // Then
            assertEquals(2, companyRepository.count())
            assertEquals(created1.companyName, retrieved1.companyName)
            assertEquals(created2.companyName, retrieved2.companyName)
        }
    }
}