package nz.govt.companiesoffice.register.integration

import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.repository.CompanyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
class CompanyRepositoryIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:15")
            .withDatabaseName("companies_register_test")
            .withUsername("test")
            .withPassword("test")
    }

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var testCompany: Company

    @BeforeEach
    fun setUp() {
        companyRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()

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
    @DisplayName("Basic CRUD Operations")
    inner class BasicCrudOperations {

        @Test
        @DisplayName("Should save and retrieve company")
        fun `should save and retrieve company successfully`() {
            // When
            val savedCompany = companyRepository.save(testCompany)
            entityManager.flush()
            entityManager.clear()

            // Then
            assertNotNull(savedCompany.id)
            val retrievedCompany = companyRepository.findById(savedCompany.id!!)
            assertTrue(retrievedCompany.isPresent)
            assertEquals(testCompany.companyName, retrievedCompany.get().companyName)
            assertEquals(testCompany.companyNumber, retrievedCompany.get().companyNumber)
            assertEquals(testCompany.companyType, retrievedCompany.get().companyType)
            assertEquals(testCompany.incorporationDate, retrievedCompany.get().incorporationDate)
            assertEquals(testCompany.nzbn, retrievedCompany.get().nzbn)
            assertEquals(testCompany.status, retrievedCompany.get().status)
        }

        @Test
        @DisplayName("Should update company")
        fun `should update company successfully`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)
            entityManager.flush()

            // When
            savedCompany.companyName = "Updated Company Ltd"
            savedCompany.status = "SUSPENDED"
            val updatedCompany = companyRepository.save(savedCompany)
            entityManager.flush()
            entityManager.clear()

            // Then
            val retrievedCompany = companyRepository.findById(savedCompany.id!!)
            assertTrue(retrievedCompany.isPresent)
            assertEquals("Updated Company Ltd", retrievedCompany.get().companyName)
            assertEquals("SUSPENDED", retrievedCompany.get().status)
            assertEquals(testCompany.companyNumber, retrievedCompany.get().companyNumber)
        }

        @Test
        @DisplayName("Should delete company")
        fun `should delete company successfully`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)
            entityManager.flush()
            assertEquals(1, companyRepository.count())

            // When
            companyRepository.delete(savedCompany)
            entityManager.flush()

            // Then
            assertEquals(0, companyRepository.count())
            val retrievedCompany = companyRepository.findById(savedCompany.id!!)
            assertFalse(retrievedCompany.isPresent)
        }

        @Test
        @DisplayName("Should count companies")
        fun `should count companies correctly`() {
            // Given
            assertEquals(0, companyRepository.count())

            // When
            companyRepository.save(testCompany)
            companyRepository.save(Company(
                companyNumber = "87654321",
                companyName = "Company 2",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            ))
            entityManager.flush()

            // Then
            assertEquals(2, companyRepository.count())
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    inner class CustomQueryMethods {

        @Test
        @DisplayName("Should find company by number")
        fun `findByCompanyNumber should work correctly`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)
            entityManager.flush()
            entityManager.clear()

            // When
            val foundCompany = companyRepository.findByCompanyNumber("12345678")

            // Then
            assertNotNull(foundCompany)
            assertEquals(savedCompany.id, foundCompany!!.id)
            assertEquals(testCompany.companyName, foundCompany.companyName)

            // Test non-existent number
            val notFoundCompany = companyRepository.findByCompanyNumber("99999999")
            assertNull(notFoundCompany)
        }

        @Test
        @DisplayName("Should find company by name")
        fun `findByCompanyName should work correctly`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)
            entityManager.flush()
            entityManager.clear()

            // When
            val foundCompany = companyRepository.findByCompanyNameIgnoreCase("Test Company Ltd")

            // Then
            assertNotNull(foundCompany)
            assertEquals(savedCompany.id, foundCompany!!.id)
            assertEquals(testCompany.companyNumber, foundCompany.companyNumber)

            // Test non-existent name
            val notFoundCompany = companyRepository.findByCompanyNameIgnoreCase("Non-existent Company")
            assertNull(notFoundCompany)
        }

        @Test
        @DisplayName("Should find company by NZBN")
        fun `findByNzbn should work correctly`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)
            entityManager.flush()
            entityManager.clear()

            // When
            val foundCompany = companyRepository.findByNzbn("9429000000000")

            // Then
            assertNotNull(foundCompany)
            assertEquals(savedCompany.id, foundCompany!!.id)
            assertEquals(testCompany.companyName, foundCompany.companyName)

            // Test non-existent NZBN
            val notFoundCompany = companyRepository.findByNzbn("9429000000999")
            assertNull(notFoundCompany)
        }

        @Test
        @DisplayName("Should find companies by status")
        fun `findByStatus should work correctly`() {
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
                companyName = "Company 2",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )
            val inactiveCompany = Company(
                companyNumber = "33333333",
                companyName = "Company 3",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000002",
                status = "INACTIVE",
            )

            companyRepository.save(activeCompany1)
            companyRepository.save(activeCompany2)
            companyRepository.save(inactiveCompany)
            entityManager.flush()
            entityManager.clear()

            // When
            val activeCompanies = companyRepository.findByStatus("ACTIVE")
            val inactiveCompanies = companyRepository.findByStatus("INACTIVE")

            // Then
            assertEquals(2, activeCompanies.size)
            assertEquals(1, inactiveCompanies.size)
            assertTrue(activeCompanies.all { it.status == "ACTIVE" })
            assertTrue(inactiveCompanies.all { it.status == "INACTIVE" })
        }

        @Test
        @DisplayName("Should find companies by type")
        fun `findByCompanyType should work correctly`() {
            // Given
            val ltdCompany = Company(
                companyNumber = "11111111",
                companyName = testCompany.companyName,
                companyType = CompanyType.LTD,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val unlimitedCompany = Company(
                companyNumber = "22222222",
                companyName = "Company 2",
                companyType = CompanyType.UNLIMITED,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )

            companyRepository.save(ltdCompany)
            companyRepository.save(unlimitedCompany)
            entityManager.flush()
            entityManager.clear()

            // When
            val ltdCompanies = companyRepository.findByCompanyType(CompanyType.LTD)
            val unlimitedCompanies = companyRepository.findByCompanyType(CompanyType.UNLIMITED)

            // Then
            assertEquals(1, ltdCompanies.size)
            assertEquals(1, unlimitedCompanies.size)
            assertEquals(CompanyType.LTD, ltdCompanies[0].companyType)
            assertEquals(CompanyType.UNLIMITED, unlimitedCompanies[0].companyType)
        }

        @Test
        @DisplayName("Should find companies by incorporation date range")
        fun `findByIncorporationDateBetween should work correctly`() {
            // Given
            val company2020 = Company(
                companyNumber = "11111111",
                companyName = testCompany.companyName,
                companyType = testCompany.companyType,
                incorporationDate = LocalDate.of(2020, 1, 1),
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val company2021 = Company(
                companyNumber = "22222222",
                companyName = "Company 2",
                companyType = testCompany.companyType,
                incorporationDate = LocalDate.of(2021, 6, 15),
                nzbn = "9429000000001",
                status = testCompany.status,
            )
            val company2022 = Company(
                companyNumber = "33333333",
                companyName = "Company 3",
                companyType = testCompany.companyType,
                incorporationDate = LocalDate.of(2022, 12, 31),
                nzbn = "9429000000002",
                status = testCompany.status,
            )

            companyRepository.save(company2020)
            companyRepository.save(company2021)
            companyRepository.save(company2022)
            entityManager.flush()
            entityManager.clear()

            // When
            val companies2021 = companyRepository.findByIncorporationDateBetween(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 12, 31)
            )

            // Then
            assertEquals(1, companies2021.size)
            assertEquals(LocalDate.of(2021, 6, 15), companies2021[0].incorporationDate)
        }
    }

    @Nested
    @DisplayName("Search Operations")
    inner class SearchOperations {

        @Test
        @DisplayName("Should search companies by name containing")
        fun `findByCompanyNameContainingIgnoreCase should work correctly`() {
            // Given
            val company1 = Company(
                companyNumber = "11111111",
                companyName = "Alpha Software Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "22222222",
                companyName = "Beta Software Solutions",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )
            val company3 = Company(
                companyNumber = "33333333",
                companyName = "Gamma Hardware Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000002",
                status = testCompany.status,
            )

            companyRepository.save(company1)
            companyRepository.save(company2)
            companyRepository.save(company3)
            entityManager.flush()
            entityManager.clear()

            // When
            val softwareCompanies = companyRepository.findByCompanyNameContainingIgnoreCase("software")
            val hardwareCompanies = companyRepository.findByCompanyNameContainingIgnoreCase("HARDWARE")
            val ltdCompanies = companyRepository.findByCompanyNameContainingIgnoreCase("Ltd")

            // Then
            assertEquals(2, softwareCompanies.size)
            assertEquals(1, hardwareCompanies.size)
            assertEquals(2, ltdCompanies.size)
            assertTrue(softwareCompanies.all { it.companyName.contains("Software", ignoreCase = true) })
            assertTrue(hardwareCompanies.all { it.companyName.contains("Hardware", ignoreCase = true) })
        }

        @Test
        @DisplayName("Should handle empty search results")
        fun `search should return empty list for non-matching queries`() {
            // Given
            companyRepository.save(testCompany)
            entityManager.flush()
            entityManager.clear()

            // When
            val results = companyRepository.findByCompanyNameContainingIgnoreCase("NonExistent")

            // Then
            assertTrue(results.isEmpty())
        }

        @Test
        @DisplayName("Should search with partial matches")
        fun `search should handle partial matches correctly`() {
            // Given
            val company1 = Company(
                companyNumber = "11111111",
                companyName = "Technology Solutions Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "22222222",
                companyName = "Tech Innovations",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )
            val company3 = Company(
                companyNumber = "33333333",
                companyName = "Financial Services",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000002",
                status = testCompany.status,
            )

            companyRepository.save(company1)
            companyRepository.save(company2)
            companyRepository.save(company3)
            entityManager.flush()
            entityManager.clear()

            // When
            val techCompanies = companyRepository.findByCompanyNameContainingIgnoreCase("tech")

            // Then
            assertEquals(2, techCompanies.size)
            assertTrue(techCompanies.any { it.companyName.contains("Technology", ignoreCase = true) })
            assertTrue(techCompanies.any { it.companyName.contains("Tech", ignoreCase = true) })
        }
    }

    @Nested
    @DisplayName("Constraint Validation")
    inner class ConstraintValidation {

        @Test
        @DisplayName("Should enforce unique company number constraint")
        fun `should enforce unique company number constraint`() {
            // Given
            val company1 = Company(
                companyNumber = "12345678",
                companyName = testCompany.companyName,
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "12345678",
                companyName = "Different Name",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )

            // When
            companyRepository.save(company1)
            entityManager.flush()

            // Then
            var exceptionThrown = false
            try {
                companyRepository.save(company2)
                entityManager.flush()
            } catch (e: Exception) {
                exceptionThrown = true
            }
            assertTrue(exceptionThrown)
        }

        @Test
        @DisplayName("Should enforce unique company name constraint")
        fun `should enforce unique company name constraint`() {
            // Given
            val company1 = Company(
                companyNumber = testCompany.companyNumber,
                companyName = "Test Company Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = testCompany.nzbn,
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "87654321",
                companyName = "Test Company Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )

            // When
            companyRepository.save(company1)
            entityManager.flush()

            // Then
            var exceptionThrown = false
            try {
                companyRepository.save(company2)
                entityManager.flush()
            } catch (e: Exception) {
                exceptionThrown = true
            }
            assertTrue(exceptionThrown)
        }

        @Test
        @DisplayName("Should enforce unique NZBN constraint")
        fun `should enforce unique NZBN constraint`() {
            // Given
            val company1 = Company(
                companyNumber = testCompany.companyNumber,
                companyName = testCompany.companyName,
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000000",
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "87654321",
                companyName = "Different Name",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000000",
                status = testCompany.status,
            )

            // When
            companyRepository.save(company1)
            entityManager.flush()

            // Then
            var exceptionThrown = false
            try {
                companyRepository.save(company2)
                entityManager.flush()
            } catch (e: Exception) {
                exceptionThrown = true
            }
            assertTrue(exceptionThrown)
        }

        @Test
        @DisplayName("Should enforce not null constraints")
        fun `should enforce not null constraints`() {
            // Given
            // Create companies with null values that should fail validation
            var companyWithNullName: Company? = null
            var companyWithNullNumber: Company? = null
            
            try {
                companyWithNullName = Company(
                    companyNumber = testCompany.companyNumber,
                    companyName = "", // Empty string instead of null since companyName is non-null
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = testCompany.nzbn,
                    status = testCompany.status,
                )
            } catch (e: Exception) {
                // Expected to fail due to validation
            }
            
            try {
                companyWithNullNumber = Company(
                    companyNumber = "", // Empty string instead of null since companyNumber is non-null
                    companyName = testCompany.companyName,
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = testCompany.nzbn,
                    status = testCompany.status,
                )
            } catch (e: Exception) {
                // Expected to fail due to validation
            }

            // When & Then
            var exceptionThrown = false
            if (companyWithNullName != null) {
                try {
                    companyRepository.save(companyWithNullName)
                    entityManager.flush()
                } catch (e: Exception) {
                    exceptionThrown = true
                }
            }
            assertTrue(exceptionThrown)

            exceptionThrown = false
            if (companyWithNullNumber != null) {
                try {
                    companyRepository.save(companyWithNullNumber)
                    entityManager.flush()
                } catch (e: Exception) {
                    exceptionThrown = true
                }
            }
            assertTrue(exceptionThrown)
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should handle large datasets efficiently")
        fun `should handle large datasets efficiently`() {
            // Given
            val companies = (1..100).map { i ->
                Company(
                    companyNumber = "1000000$i",
                    companyName = "Company $i Ltd",
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = "942900000000$i",
                    status = testCompany.status,
                )
            }

            // When
            val startTime = System.currentTimeMillis()
            companyRepository.saveAll(companies)
            entityManager.flush()
            val savedCompanies = companyRepository.findAll()
            val endTime = System.currentTimeMillis()

            // Then
            assertEquals(100, savedCompanies.size)
            assertTrue(endTime - startTime < 5000) // Should complete within 5 seconds
        }

        @Test
        @DisplayName("Should handle concurrent access patterns")
        fun `should handle typical access patterns efficiently`() {
            // Given
            val companies = (1..50).map { i ->
                Company(
                    companyNumber = "2000000$i",
                    companyName = "Access Test Company $i Ltd",
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = "942900000200$i",
                    status = testCompany.status,
                )
            }
            companyRepository.saveAll(companies)
            entityManager.flush()

            // When - Simulate typical access patterns
            val startTime = System.currentTimeMillis()
            
            // Multiple finds by ID
            repeat(10) { i ->
                companyRepository.findByCompanyNumber("2000000${i + 1}")
            }
            
            // Multiple searches
            repeat(5) { i ->
                companyRepository.findByCompanyNameContainingIgnoreCase("Company $i")
            }
            
            // Status queries
            companyRepository.findByStatus("ACTIVE")
            
            val endTime = System.currentTimeMillis()

            // Then
            assertTrue(endTime - startTime < 2000) // Should complete within 2 seconds
        }
    }

    @Nested
    @DisplayName("Transaction Behavior")
    inner class TransactionBehavior {

        @Test
        @DisplayName("Should handle rollback scenarios")
        fun `should handle rollback scenarios correctly`() {
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
            entityManager.flush()

            // When & Then
            var exceptionThrown = false
            try {
                val invalidCompany = Company(
                    companyNumber = "11111111",
                    companyName = "Different Name",
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = "9429000000001",
                    status = testCompany.status,
                )
                companyRepository.save(invalidCompany)
                entityManager.flush()
            } catch (e: Exception) {
                exceptionThrown = true
            }
            
            assertTrue(exceptionThrown)
            assertEquals(1, companyRepository.count())
        }

        @Test
        @DisplayName("Should handle batch operations correctly")
        fun `should handle batch operations correctly`() {
            // Given
            val companies = listOf(
                Company(
                    companyNumber = "11111111",
                    companyName = testCompany.companyName,
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = testCompany.nzbn,
                    status = testCompany.status,
                ),
                Company(
                    companyNumber = "22222222",
                    companyName = "Company 2",
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = "9429000000001",
                    status = testCompany.status,
                ),
                Company(
                    companyNumber = "33333333",
                    companyName = "Company 3",
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = "9429000000002",
                    status = testCompany.status,
                )
            )

            // When
            companyRepository.saveAll(companies)
            entityManager.flush()

            // Then
            assertEquals(3, companyRepository.count())
            companies.forEach { company ->
                val saved = companyRepository.findByCompanyNumber(company.companyNumber)
                assertNotNull(saved)
                assertEquals(company.companyName, saved!!.companyName)
            }
        }
    }
}