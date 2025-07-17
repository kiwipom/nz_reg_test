package nz.govt.companiesoffice.register.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import nz.govt.companiesoffice.register.config.SecurityConfig
import nz.govt.companiesoffice.register.config.TestSecurityConfig
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.entity.DirectorStatus
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.service.DirectorDisqualificationService
import nz.govt.companiesoffice.register.service.DirectorService
import nz.govt.companiesoffice.register.service.ResidencyValidationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(DirectorController::class)
@Import(SecurityConfig::class, TestSecurityConfig::class)
class DirectorControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var directorService: DirectorService

    @MockkBean
    private lateinit var residencyValidationService: ResidencyValidationService

    @MockkBean
    private lateinit var disqualificationService: DirectorDisqualificationService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
    @WithMockUser(roles = ["ADMIN"])
    fun `should appoint director successfully`() {
        every { directorService.appointDirector(any()) } returns testDirector

        mockMvc.perform(
            post("/v1/directors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDirector)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.fullName").value("John Doe"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        verify { directorService.appointDirector(any()) }
    }

    @Test
    fun `should require authentication for director appointment`() {
        mockMvc.perform(
            post("/v1/directors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDirector)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["PUBLIC"])
    fun `should require ADMIN or REGISTRAR role for director appointment`() {
        mockMvc.perform(
            post("/v1/directors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDirector)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should get director by id successfully`() {
        every { directorService.getDirectorById(1L) } returns testDirector

        mockMvc.perform(get("/v1/directors/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.fullName").value("John Doe"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `should return 404 when director not found`() {
        every { directorService.getDirectorById(1L) } throws ResourceNotFoundException("director", "Director not found")

        mockMvc.perform(get("/v1/directors/1"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
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
        every { directorService.updateDirector(1L, any()) } returns updatedDirector

        mockMvc.perform(
            put("/v1/directors/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDirector)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Jane Doe"))

        verify { directorService.updateDirector(1L, any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should resign director successfully`() {
        val resignationDate = LocalDate.now()
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
        every { directorService.resignDirector(1L, resignationDate) } returns resignedDirector

        mockMvc.perform(
            post("/v1/directors/1/resign")
                .param("resignationDate", resignationDate.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESIGNED"))
            .andExpect(jsonPath("$.resignedDate").value(resignationDate.toString()))

        verify { directorService.resignDirector(1L, resignationDate) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should resign director with default date when no date provided`() {
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
        every { directorService.resignDirector(1L, any()) } returns resignedDirector

        mockMvc.perform(post("/v1/directors/1/resign"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESIGNED"))

        verify { directorService.resignDirector(1L, any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should handle validation error when resigning director`() {
        every { directorService.resignDirector(1L, any()) } throws ValidationException(
            "resignation",
            "Cannot resign - company must have at least one director",
        )

        mockMvc.perform(post("/v1/directors/1/resign"))
            .andExpect(status().isBadRequest)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("at least one director")))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should give director consent successfully`() {
        val consentDate = LocalDate.now()
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
        every { directorService.giveDirectorConsent(1L, consentDate) } returns consentedDirector

        mockMvc.perform(
            post("/v1/directors/1/consent")
                .param("consentDate", consentDate.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.consentGiven").value(true))
            .andExpect(jsonPath("$.consentDate").value(consentDate.toString()))

        verify { directorService.giveDirectorConsent(1L, consentDate) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should disqualify director successfully`() {
        val reason = "Court order"
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
        every { directorService.disqualifyDirector(1L, reason) } returns disqualifiedDirector

        mockMvc.perform(
            post("/v1/directors/1/disqualify")
                .param("reason", reason),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DISQUALIFIED"))

        verify { directorService.disqualifyDirector(1L, reason) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should delete director successfully`() {
        every { directorService.deleteDirector(1L) } returns Unit

        mockMvc.perform(delete("/v1/directors/1"))
            .andExpect(status().isNoContent)

        verify { directorService.deleteDirector(1L) }
    }

    @Test
    @WithMockUser(roles = ["REGISTRAR"])
    fun `should require ADMIN role for director deletion`() {
        mockMvc.perform(delete("/v1/directors/1"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should get directors by company`() {
        val directors = listOf(testDirector)
        every { directorService.getDirectorsByCompany(1L) } returns directors

        mockMvc.perform(get("/v1/directors/company/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").value(1))
    }

    @Test
    fun `should get active directors by company`() {
        val directors = listOf(testDirector)
        every { directorService.getActiveDirectorsByCompany(1L) } returns directors

        mockMvc.perform(get("/v1/directors/company/1/active"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].status").value("ACTIVE"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should get directors requiring consent`() {
        val directors = listOf(testDirector)
        every { directorService.getDirectorsRequiringConsent(1L) } returns directors

        mockMvc.perform(get("/v1/directors/company/1/requiring-consent"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `should search directors`() {
        val directors = listOf(testDirector)
        every { directorService.searchDirectors("John") } returns directors

        mockMvc.perform(
            get("/v1/directors/search")
                .param("query", "John"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].fullName").value("John Doe"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should validate company director compliance`() {
        val compliance = mapOf(
            "hasMinimumDirectors" to true,
            "hasResidentDirector" to true,
            "allDirectorsHaveConsent" to true,
        )
        every { directorService.validateCompanyDirectorCompliance(1L) } returns compliance

        mockMvc.perform(get("/v1/directors/company/1/compliance"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasMinimumDirectors").value(true))
            .andExpect(jsonPath("$.hasResidentDirector").value(true))
            .andExpect(jsonPath("$.allDirectorsHaveConsent").value(true))
    }
}
