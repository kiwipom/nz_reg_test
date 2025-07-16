package nz.govt.companiesoffice.register.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.verify
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.service.CompanyService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(CompanyController::class)
class CompanyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var companyService: CompanyService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var testCompany: Company
    private lateinit var testCompanyList: List<Company>

    @BeforeEach
    fun setUp() {
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
    @DisplayName("Public Endpoint Tests")
    inner class PublicEndpointTests {

        @Test
        @DisplayName("Should allow public access to company search")
        fun `GET search should allow public access`() {
            // Given
            val query = "Test Company"
            every { companyService.searchCompanies(query) } returns testCompanyList

            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/search")
                    .param("query", query),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].companyName").value("Test Company Ltd"))
                .andExpect(jsonPath("$[1].companyName").value("Another Company Ltd"))

            verify { companyService.searchCompanies(query) }
        }

        @Test
        @DisplayName("Should allow public access to name availability check")
        fun `GET check-name should allow public access`() {
            // Given
            val companyName = "Available Company"
            every { companyService.isCompanyNameAvailable(companyName) } returns true

            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/check-name")
                    .param("name", companyName),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available").value(true))

            verify { companyService.isCompanyNameAvailable(companyName) }
        }

        @Test
        @DisplayName("Should allow public access to number availability check")
        fun `GET check-number should allow public access`() {
            // Given
            val companyNumber = "99999999"
            every { companyService.isCompanyNumberAvailable(companyNumber) } returns true

            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/check-number")
                    .param("number", companyNumber),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available").value(true))

            verify { companyService.isCompanyNumberAvailable(companyNumber) }
        }

        @Test
        @DisplayName("Should handle search with no results")
        fun `GET search should handle no results`() {
            // Given
            val query = "NonExistent Company"
            every { companyService.searchCompanies(query) } returns emptyList()

            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/search")
                    .param("query", query),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0))

            verify { companyService.searchCompanies(query) }
        }
    }

    @Nested
    @DisplayName("Authenticated Endpoint Tests")
    inner class AuthenticatedEndpointTests {

        @Test
        @DisplayName("Should require authentication for getting company by ID")
        fun `GET company by ID should require authentication`() {
            // When & Then
            mockMvc.perform(get("/api/v1/companies/1"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        @DisplayName("Should allow authenticated access to get company by ID")
        fun `GET company by ID should allow authenticated access`() {
            // Given
            val companyId = 1L
            every { companyService.getCompanyById(companyId) } returns testCompany

            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/$companyId")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Test Company Ltd"))
                .andExpect(jsonPath("$.companyNumber").value("12345678"))

            verify { companyService.getCompanyById(companyId) }
        }

        @Test
        @DisplayName("Should handle company not found")
        fun `GET company by ID should handle not found`() {
            // Given
            val companyId = 999L
            every { companyService.getCompanyById(companyId) } throws ResourceNotFoundException(
                "company", "Company not found",
            )

            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/$companyId")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isNotFound)

            verify { companyService.getCompanyById(companyId) }
        }

        @Test
        @DisplayName("Should allow authenticated access to get company by number")
        fun `GET company by number should allow authenticated access`() {
            // Given
            val companyNumber = "12345678"
            every { companyService.getCompanyByNumber(companyNumber) } returns testCompany

            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/number/$companyNumber")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.companyNumber").value(companyNumber))

            verify { companyService.getCompanyByNumber(companyNumber) }
        }

        @Test
        @DisplayName("Should allow authenticated access to get active companies")
        fun `GET active companies should allow authenticated access`() {
            // Given
            every { companyService.getActiveCompanies() } returns testCompanyList

            // When & Then
            mockMvc.perform(
                get("/api/v1/companies")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))

            verify { companyService.getActiveCompanies() }
        }
    }

    @Nested
    @DisplayName("Role-Based Access Control Tests")
    inner class RoleBasedAccessControlTests {

        @Test
        @DisplayName("Should allow ADMIN to create company")
        fun `POST company should allow ADMIN role`() {
            // Given
            val newCompany = testCompany.copy(id = null)
            val savedCompany = testCompany.copy()
            every { companyService.createCompany(any()) } returns savedCompany

            // When & Then
            mockMvc.perform(
                post("/api/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(listOf("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newCompany)),
            )
                .andExpect(status().isCreated)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Test Company Ltd"))

            verify { companyService.createCompany(any()) }
        }

        @Test
        @DisplayName("Should allow REGISTRAR to create company")
        fun `POST company should allow REGISTRAR role`() {
            // Given
            val newCompany = testCompany.copy(id = null)
            val savedCompany = testCompany.copy()
            every { companyService.createCompany(any()) } returns savedCompany

            // When & Then
            mockMvc.perform(
                post("/api/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|registrar123") }
                            .authorities(listOf("ROLE_REGISTRAR")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newCompany)),
            )
                .andExpect(status().isCreated)

            verify { companyService.createCompany(any()) }
        }

        @Test
        @DisplayName("Should deny PUBLIC user to create company")
        fun `POST company should deny PUBLIC role`() {
            // Given
            val newCompany = testCompany.copy(id = null)

            // When & Then
            mockMvc.perform(
                post("/api/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|public123") }
                            .authorities(listOf("ROLE_PUBLIC")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newCompany)),
            )
                .andExpect(status().isForbidden)

            verify(exactly = 0) { companyService.createCompany(any()) }
        }

        @Test
        @DisplayName("Should allow ADMIN to update company")
        fun `PUT company should allow ADMIN role`() {
            // Given
            val companyId = 1L
            val updatedCompany = testCompany.copy(companyName = "Updated Company")
            every { companyService.updateCompany(companyId, any()) } returns updatedCompany

            // When & Then
            mockMvc.perform(
                put("/api/v1/companies/$companyId")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(listOf("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedCompany)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.companyName").value("Updated Company"))

            verify { companyService.updateCompany(companyId, any()) }
        }

        @Test
        @DisplayName("Should allow REGISTRAR to update company")
        fun `PUT company should allow REGISTRAR role`() {
            // Given
            val companyId = 1L
            val updatedCompany = testCompany.copy(companyName = "Updated Company")
            every { companyService.updateCompany(companyId, any()) } returns updatedCompany

            // When & Then
            mockMvc.perform(
                put("/api/v1/companies/$companyId")
                    .with(
                        jwt().jwt { it.subject("auth0|registrar123") }
                            .authorities(listOf("ROLE_REGISTRAR")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedCompany)),
            )
                .andExpect(status().isOk)

            verify { companyService.updateCompany(companyId, any()) }
        }

        @Test
        @DisplayName("Should deny INTERNAL_OPS to update company")
        fun `PUT company should deny INTERNAL_OPS role`() {
            // Given
            val companyId = 1L
            val updatedCompany = testCompany.copy(companyName = "Updated Company")

            // When & Then
            mockMvc.perform(
                put("/api/v1/companies/$companyId")
                    .with(
                        jwt().jwt { it.subject("auth0|internal123") }
                            .authorities(listOf("ROLE_INTERNAL_OPS")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedCompany)),
            )
                .andExpect(status().isForbidden)

            verify(exactly = 0) { companyService.updateCompany(any(), any()) }
        }

        @Test
        @DisplayName("Should allow only ADMIN to delete company")
        fun `DELETE company should allow only ADMIN role`() {
            // Given
            val companyId = 1L
            every { companyService.deleteCompany(companyId) } returns Unit

            // When & Then
            mockMvc.perform(
                delete("/api/v1/companies/$companyId")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(listOf("ROLE_ADMIN")),
                    ),
            )
                .andExpect(status().isNoContent)

            verify { companyService.deleteCompany(companyId) }
        }

        @Test
        @DisplayName("Should deny REGISTRAR to delete company")
        fun `DELETE company should deny REGISTRAR role`() {
            // Given
            val companyId = 1L

            // When & Then
            mockMvc.perform(
                delete("/api/v1/companies/$companyId")
                    .with(
                        jwt().jwt { it.subject("auth0|registrar123") }
                            .authorities(listOf("ROLE_REGISTRAR")),
                    ),
            )
                .andExpect(status().isForbidden)

            verify(exactly = 0) { companyService.deleteCompany(any()) }
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    inner class ValidationTests {

        @Test
        @DisplayName("Should handle validation errors on create")
        fun `POST company should handle validation errors`() {
            // Given
            val invalidCompany = testCompany.copy(id = null)
            every { companyService.createCompany(any()) } throws ValidationException(
                "companyName", "Company name already exists",
            )

            // When & Then
            mockMvc.perform(
                post("/api/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(listOf("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidCompany)),
            )
                .andExpect(status().isBadRequest)

            verify { companyService.createCompany(any()) }
        }

        @Test
        @DisplayName("Should handle invalid JSON")
        fun `POST company should handle invalid JSON`() {
            // When & Then
            mockMvc.perform(
                post("/api/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(listOf("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("invalid json"),
            )
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { companyService.createCompany(any()) }
        }

        @Test
        @DisplayName("Should handle missing required fields")
        fun `POST company should handle missing required fields`() {
            // Given
            val incompleteCompany = mapOf("companyName" to "Test Company")

            // When & Then
            mockMvc.perform(
                post("/api/v1/companies")
                    .with(
                        jwt().jwt { it.subject("auth0|admin123") }
                            .authorities(listOf("ROLE_ADMIN")),
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(incompleteCompany)),
            )
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { companyService.createCompany(any()) }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle service exceptions")
        fun `GET company should handle service exceptions`() {
            // Given
            val companyId = 1L
            every { companyService.getCompanyById(companyId) } throws RuntimeException("Database error")

            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/$companyId")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isInternalServerError)

            verify { companyService.getCompanyById(companyId) }
        }

        @Test
        @DisplayName("Should handle invalid path parameters")
        fun `GET company should handle invalid path parameters`() {
            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/invalid-id")
                    .with(jwt().jwt { it.subject("auth0|user123") }),
            )
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { companyService.getCompanyById(any()) }
        }

        @Test
        @DisplayName("Should handle missing query parameters")
        fun `GET search should handle missing query parameter`() {
            // When & Then
            mockMvc.perform(
                get("/api/v1/companies/search"),
                // Missing query parameter
            )
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { companyService.searchCompanies(any()) }
        }
    }
}
