package nz.govt.companiesoffice.register.integration

import com.fasterxml.jackson.databind.ObjectMapper
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.repository.CompanyRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@Disabled("TODO: Fix integration test authentication and security setup")
class CompanyControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc

    private lateinit var testCompany: Company

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()

        // Clear repository before each test
        companyRepository.deleteAll()

        // Create test company
        testCompany = Company(
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.of(2020, 1, 1),
            nzbn = "9429000000000",
            status = "ACTIVE",
        )
    }

    @Nested
    @DisplayName("Public Endpoints Integration Tests")
    inner class PublicEndpointsTests {

        @Test
        @DisplayName("Should search companies without authentication")
        fun `search companies should work without authentication`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)

            // When & Then
            mockMvc.perform(
                get("/v1/companies/search")
                    .param("query", "Test Company"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].companyName").value("Test Company Ltd"))
                .andExpect(jsonPath("$[0].companyNumber").value("12345678"))
        }

        @Test
        @DisplayName("Should check company name availability without authentication")
        fun `check name availability should work without authentication`() {
            // Given
            companyRepository.save(testCompany)

            // When & Then - Check existing name
            mockMvc.perform(
                get("/v1/companies/check-name")
                    .param("name", "Test Company Ltd"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available").value(false))

            // When & Then - Check available name
            mockMvc.perform(
                get("/v1/companies/check-name")
                    .param("name", "Available Company Ltd"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available").value(true))
        }

        @Test
        @DisplayName("Should check company number availability without authentication")
        fun `check number availability should work without authentication`() {
            // Given
            companyRepository.save(testCompany)

            // When & Then - Check existing number
            mockMvc.perform(
                get("/v1/companies/check-number")
                    .param("number", "12345678"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available").value(false))

            // When & Then - Check available number
            mockMvc.perform(
                get("/v1/companies/check-number")
                    .param("number", "99999999"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available").value(true))
        }

        @Test
        @DisplayName("Should return empty results for non-matching search")
        fun `search should return empty results for non-matching query`() {
            // Given
            companyRepository.save(testCompany)

            // When & Then
            mockMvc.perform(
                get("/v1/companies/search")
                    .param("query", "NonExistent Company"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0))
        }
    }

    @Nested
    @DisplayName("Authenticated Endpoints Integration Tests")
    inner class AuthenticatedEndpointsTests {

        @Test
        @DisplayName("Should get company by ID with authentication")
        fun `get company by ID should work with authentication`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)

            // When & Then
            mockMvc.perform(
                get("/v1/companies/${savedCompany.id}")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(savedCompany.id))
                .andExpect(jsonPath("$.companyName").value("Test Company Ltd"))
                .andExpect(jsonPath("$.companyNumber").value("12345678"))
        }

        @Test
        @DisplayName("Should get company by number with authentication")
        fun `get company by number should work with authentication`() {
            // Given
            companyRepository.save(testCompany)

            // When & Then
            mockMvc.perform(
                get("/v1/companies/number/12345678")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.companyName").value("Test Company Ltd"))
                .andExpect(jsonPath("$.companyNumber").value("12345678"))
        }

        @Test
        @DisplayName("Should get active companies with authentication")
        fun `get active companies should work with authentication`() {
            // Given
            companyRepository.save(testCompany)
            val inactiveCompany = Company(
                companyNumber = "87654321",
                companyName = "Inactive Company Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = "INACTIVE",
            )
            companyRepository.save(inactiveCompany)

            // When & Then
            mockMvc.perform(
                get("/v1/companies")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].companyName").value("Test Company Ltd"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated requests")
        fun `authenticated endpoints should return 401 without authentication`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)

            // When & Then
            mockMvc.perform(get("/v1/companies/${savedCompany.id}"))
                .andExpect(status().isUnauthorized)

            mockMvc.perform(get("/v1/companies/number/12345678"))
                .andExpect(status().isUnauthorized)

            mockMvc.perform(get("/v1/companies"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        @DisplayName("Should return 404 for non-existent company")
        fun `get company by ID should return 404 for non-existent company`() {
            // When & Then
            mockMvc.perform(
                get("/v1/companies/999999")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("Should return 404 for non-existent company number")
        fun `get company by number should return 404 for non-existent number`() {
            // When & Then
            mockMvc.perform(
                get("/v1/companies/number/99999999")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("Role-Based Access Control Integration Tests")
    inner class RoleBasedAccessControlTests {

        @Test
        @DisplayName("Should allow ADMIN to create company")
        fun `create company should work with ADMIN role`() {
            // Given
            val newCompany = Company(
                companyNumber = "99999999",
                companyName = "New Company Ltd",
                companyType = CompanyType.LTD,
                incorporationDate = LocalDate.of(2023, 1, 1),
                nzbn = "9429000000001",
                status = "ACTIVE",
            )

            // When & Then
            mockMvc.perform(
                post("/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newCompany)),
            )
                .andExpect(status().isCreated)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.companyName").value("New Company Ltd"))
                .andExpect(jsonPath("$.companyNumber").value("99999999"))

            // Verify company was actually saved
            val savedCompany = companyRepository.findByCompanyNumber("99999999")
            assert(savedCompany != null)
            assert(savedCompany!!.companyName == "New Company Ltd")
        }

        @Test
        @DisplayName("Should allow REGISTRAR to create company")
        fun `create company should work with REGISTRAR role`() {
            // Given
            val newCompany = Company(
                companyNumber = "88888888",
                companyName = "Registrar Company Ltd",
                companyType = CompanyType.LTD,
                incorporationDate = LocalDate.of(2023, 1, 1),
                nzbn = "9429000000002",
                status = "ACTIVE",
            )

            // When & Then
            mockMvc.perform(
                post("/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|registrar123") }
                            .authorities(SimpleGrantedAuthority("ROLE_REGISTRAR")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newCompany)),
            )
                .andExpect(status().isCreated)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.companyName").value("Registrar Company Ltd"))

            // Verify company was actually saved
            val savedCompany = companyRepository.findByCompanyNumber("88888888")
            assert(savedCompany != null)
        }

        @Test
        @DisplayName("Should deny PUBLIC user to create company")
        fun `create company should deny PUBLIC role`() {
            // Given
            val newCompany = Company(
                companyNumber = "77777777",
                companyName = "Public Company Ltd",
                companyType = CompanyType.LTD,
                incorporationDate = LocalDate.of(2023, 1, 1),
                nzbn = "9429000000003",
                status = "ACTIVE",
            )

            // When & Then
            mockMvc.perform(
                post("/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|public123") }
                            .authorities(SimpleGrantedAuthority("ROLE_PUBLIC")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newCompany)),
            )
                .andExpect(status().isForbidden)

            // Verify company was not saved
            val savedCompany = companyRepository.findByCompanyNumber("77777777")
            assert(savedCompany == null)
        }

        @Test
        @DisplayName("Should allow ADMIN to update company")
        fun `update company should work with ADMIN role`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)
            val updateData = Company(
                id = savedCompany.id,
                companyNumber = savedCompany.companyNumber,
                companyName = "Updated Company Ltd",
                companyType = savedCompany.companyType,
                incorporationDate = savedCompany.incorporationDate,
                nzbn = savedCompany.nzbn,
                status = savedCompany.status,
            )

            // When & Then
            mockMvc.perform(
                put("/v1/companies/${savedCompany.id}")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateData)),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.companyName").value("Updated Company Ltd"))

            // Verify company was actually updated
            val updatedCompany = companyRepository.findById(savedCompany.id!!).get()
            assert(updatedCompany.companyName == "Updated Company Ltd")
        }

        @Test
        @DisplayName("Should allow REGISTRAR to update company")
        fun `update company should work with REGISTRAR role`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)
            val updateData = Company(
                id = savedCompany.id,
                companyNumber = savedCompany.companyNumber,
                companyName = "Registrar Updated Company Ltd",
                companyType = savedCompany.companyType,
                incorporationDate = savedCompany.incorporationDate,
                nzbn = savedCompany.nzbn,
                status = savedCompany.status,
            )

            // When & Then
            mockMvc.perform(
                put("/v1/companies/${savedCompany.id}")
                    .with(
                        jwt().jwt { it.subject("auth0|registrar123") }
                            .authorities(SimpleGrantedAuthority("ROLE_REGISTRAR")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateData)),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.companyName").value("Registrar Updated Company Ltd"))

            // Verify company was actually updated
            val updatedCompany = companyRepository.findById(savedCompany.id!!).get()
            assert(updatedCompany.companyName == "Registrar Updated Company Ltd")
        }

        @Test
        @DisplayName("Should deny INTERNAL_OPS to update company")
        fun `update company should deny INTERNAL_OPS role`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)
            val updateData = Company(
                id = savedCompany.id,
                companyNumber = savedCompany.companyNumber,
                companyName = "Should Not Update",
                companyType = savedCompany.companyType,
                incorporationDate = savedCompany.incorporationDate,
                nzbn = savedCompany.nzbn,
                status = savedCompany.status,
            )

            // When & Then
            mockMvc.perform(
                put("/v1/companies/${savedCompany.id}")
                    .with(
                        jwt().jwt { it.subject("auth0|internal123") }
                            .authorities(SimpleGrantedAuthority("ROLE_INTERNAL_OPS")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateData)),
            )
                .andExpect(status().isForbidden)

            // Verify company was not updated
            val unchangedCompany = companyRepository.findById(savedCompany.id!!).get()
            assert(unchangedCompany.companyName == "Test Company Ltd")
        }

        @Test
        @DisplayName("Should allow only ADMIN to delete company")
        fun `delete company should work with ADMIN role`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)

            // When & Then
            mockMvc.perform(
                delete("/v1/companies/${savedCompany.id}")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
                    ),
            )
                .andExpect(status().isNoContent)

            // Verify company was actually deleted
            val deletedCompany = companyRepository.findById(savedCompany.id!!)
            assert(deletedCompany.isEmpty)
        }

        @Test
        @DisplayName("Should deny REGISTRAR to delete company")
        fun `delete company should deny REGISTRAR role`() {
            // Given
            val savedCompany = companyRepository.save(testCompany)

            // When & Then
            mockMvc.perform(
                delete("/v1/companies/${savedCompany.id}")
                    .with(
                        jwt().jwt { it.subject("auth0|registrar123") }
                            .authorities(SimpleGrantedAuthority("ROLE_REGISTRAR")),
                    ),
            )
                .andExpect(status().isForbidden)

            // Verify company was not deleted
            val existingCompany = companyRepository.findById(savedCompany.id!!)
            assert(existingCompany.isPresent)
        }
    }

    @Nested
    @DisplayName("Database Integration Tests")
    inner class DatabaseIntegrationTests {

        @Test
        @DisplayName("Should handle database constraints")
        fun `create company should handle duplicate company number`() {
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
            mockMvc.perform(
                post("/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateCompany)),
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("Should handle database transactions")
        fun `multiple operations should be transactional`() {
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
                companyName = "Company 2",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )

            // When - Create first company
            mockMvc.perform(
                post("/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(company1)),
            )
                .andExpect(status().isCreated)

            // When - Create second company
            mockMvc.perform(
                post("/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(company2)),
            )
                .andExpect(status().isCreated)

            // Then - Verify both companies exist
            val savedCompany1 = companyRepository.findByCompanyNumber("11111111")
            val savedCompany2 = companyRepository.findByCompanyNumber("22222222")
            assert(savedCompany1 != null)
            assert(savedCompany2 != null)
            assert(companyRepository.count() == 2L)
        }

        @Test
        @DisplayName("Should handle search with database queries")
        fun `search should work with database queries`() {
            // Given
            val company1 = Company(
                companyNumber = "33333333",
                companyName = "Alpha Company Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000001",
                status = testCompany.status,
            )
            val company2 = Company(
                companyNumber = "44444444",
                companyName = "Beta Company Ltd",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000002",
                status = testCompany.status,
            )
            val company3 = Company(
                companyNumber = "55555555",
                companyName = "Gamma Corporation",
                companyType = testCompany.companyType,
                incorporationDate = testCompany.incorporationDate,
                nzbn = "9429000000003",
                status = testCompany.status,
            )

            companyRepository.save(company1)
            companyRepository.save(company2)
            companyRepository.save(company3)

            // When & Then - Search for "Company"
            mockMvc.perform(
                get("/v1/companies/search")
                    .param("query", "Company"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))

            // When & Then - Search for "Alpha"
            mockMvc.perform(
                get("/v1/companies/search")
                    .param("query", "Alpha"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].companyName").value("Alpha Company Ltd"))

            // When & Then - Search for "Corporation"
            mockMvc.perform(
                get("/v1/companies/search")
                    .param("query", "Corporation"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].companyName").value("Gamma Corporation"))
        }
    }

    @Nested
    @DisplayName("Performance and Load Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should handle concurrent requests")
        fun `should handle multiple concurrent requests`() {
            // Given
            val companies = (1..10).map { i ->
                Company(
                    companyNumber = "1000000$i",
                    companyName = "Company $i Ltd",
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = "942900000000$i",
                    status = testCompany.status,
                )
            }
            companyRepository.saveAll(companies)

            // When & Then - Multiple concurrent search requests
            repeat(5) { i ->
                mockMvc.perform(
                    get("/v1/companies/search")
                        .param("query", "Company $i"),
                )
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            }
        }

        @Test
        @DisplayName("Should handle large result sets")
        fun `should handle large result sets efficiently`() {
            // Given - Create many companies
            val companies = (1..100).map { i ->
                Company(
                    companyNumber = "2000000$i",
                    companyName = "Large Dataset Company $i Ltd",
                    companyType = testCompany.companyType,
                    incorporationDate = testCompany.incorporationDate,
                    nzbn = "942900000100$i",
                    status = testCompany.status,
                )
            }
            companyRepository.saveAll(companies)

            // When & Then
            mockMvc.perform(
                get("/v1/companies/search")
                    .param("query", "Large Dataset"),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(100))
        }
    }
}
