package nz.govt.companiesoffice.register.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import nz.govt.companiesoffice.register.config.SecurityConfig
import nz.govt.companiesoffice.register.config.TestSecurityConfig
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.entity.Shareholder
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.service.ShareholderService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.context.support.WithMockUser
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

@WebMvcTest(ShareholderController::class)
@Import(SecurityConfig::class, TestSecurityConfig::class)
class ShareholderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var shareholderService: ShareholderService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
            addressLine2 = "Apt 1",
            city = "Auckland",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            isIndividual = true,
        )
    }

    @Test
    fun `should create shareholder successfully`() {
        every { shareholderService.createShareholder(any()) } returns testShareholder

        mockMvc.perform(
            post("/v1/shareholders")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testShareholder)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.fullName").value("John Smith"))
            .andExpect(jsonPath("$.isIndividual").value(true))
            .andExpect(jsonPath("$.city").value("Auckland"))
            .andExpect(jsonPath("$.country").value("NZ"))

        verify { shareholderService.createShareholder(any()) }
    }

    @Test
    fun `should require authentication for shareholder creation`() {
        mockMvc.perform(
            post("/v1/shareholders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testShareholder)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["PUBLIC"])
    fun `should require ADMIN or REGISTRAR role for shareholder creation`() {
        mockMvc.perform(
            post("/v1/shareholders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testShareholder)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["REGISTRAR"])
    fun `should allow REGISTRAR role for shareholder creation`() {
        every { shareholderService.createShareholder(any()) } returns testShareholder

        mockMvc.perform(
            post("/v1/shareholders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testShareholder)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.fullName").value("John Smith"))

        verify { shareholderService.createShareholder(any()) }
    }

    @Test
    fun `should get shareholder by id successfully`() {
        every { shareholderService.getShareholderById(1L) } returns testShareholder

        mockMvc.perform(
            get("/v1/shareholders/1")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.fullName").value("John Smith"))
            .andExpect(jsonPath("$.isIndividual").value(true))
    }

    @Test
    fun `should allow public access to get shareholder by id`() {
        every { shareholderService.getShareholderById(1L) } returns testShareholder

        mockMvc.perform(get("/v1/shareholders/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("John Smith"))
    }

    @Test
    fun `should return 404 when shareholder not found`() {
        every {
            shareholderService.getShareholderById(1L)
        } throws ResourceNotFoundException("shareholder", "Shareholder not found")

        mockMvc.perform(
            get("/v1/shareholders/1")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN"))),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should update shareholder successfully`() {
        val updatedShareholder = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "Jane Smith",
            addressLine1 = "123 Main St",
            addressLine2 = "Apt 1",
            city = "Auckland",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            isIndividual = true,
        )
        every { shareholderService.updateShareholder(1L, any()) } returns updatedShareholder

        mockMvc.perform(
            put("/v1/shareholders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedShareholder)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Jane Smith"))

        verify { shareholderService.updateShareholder(1L, any()) }
    }

    @Test
    @WithMockUser(roles = ["REGISTRAR"])
    fun `should allow REGISTRAR role for shareholder update`() {
        val updatedShareholder = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "Jane Smith",
            addressLine1 = "123 Main St",
            addressLine2 = "Apt 1",
            city = "Auckland",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            isIndividual = true,
        )
        every { shareholderService.updateShareholder(1L, any()) } returns updatedShareholder

        mockMvc.perform(
            put("/v1/shareholders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedShareholder)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Jane Smith"))
    }

    @Test
    @WithMockUser(roles = ["PUBLIC"])
    fun `should require ADMIN or REGISTRAR role for shareholder update`() {
        mockMvc.perform(
            put("/v1/shareholders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testShareholder)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should delete shareholder successfully`() {
        every { shareholderService.deleteShareholder(1L) } returns Unit

        mockMvc.perform(delete("/v1/shareholders/1"))
            .andExpect(status().isNoContent)

        verify { shareholderService.deleteShareholder(1L) }
    }

    @Test
    @WithMockUser(roles = ["REGISTRAR"])
    fun `should require ADMIN role for shareholder deletion`() {
        mockMvc.perform(delete("/v1/shareholders/1"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should get shareholders by company`() {
        val shareholders = listOf(testShareholder)
        every { shareholderService.getShareholdersByCompany(1L) } returns shareholders

        mockMvc.perform(
            get("/v1/shareholders/company/1")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].fullName").value("John Smith"))
    }

    @Test
    fun `should allow public access to get shareholders by company`() {
        val shareholders = listOf(testShareholder)
        every { shareholderService.getShareholdersByCompany(1L) } returns shareholders

        mockMvc.perform(get("/v1/shareholders/company/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `should get individual shareholders by company`() {
        val shareholders = listOf(testShareholder)
        every { shareholderService.getIndividualShareholdersByCompany(1L) } returns shareholders

        mockMvc.perform(get("/v1/shareholders/company/1/individual"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].isIndividual").value(true))
    }

    @Test
    fun `should get corporate shareholders by company`() {
        val corporateShareholder = Shareholder(
            id = 1L,
            company = testCompany,
            fullName = "ABC Corp Ltd",
            addressLine1 = "123 Corporate Ave",
            addressLine2 = "Suite 100",
            city = "Auckland",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            isIndividual = false,
        )
        val shareholders = listOf(corporateShareholder)
        every { shareholderService.getCorporateShareholdersByCompany(1L) } returns shareholders

        mockMvc.perform(get("/v1/shareholders/company/1/corporate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].isIndividual").value(false))
    }

    @Test
    fun `should get shareholders by location`() {
        val shareholders = listOf(testShareholder)
        every { shareholderService.getShareholdersByLocation(1L, "Auckland", "NZ") } returns shareholders

        mockMvc.perform(
            get("/v1/shareholders/company/1/by-location")
                .param("city", "Auckland")
                .param("country", "NZ"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].city").value("Auckland"))
            .andExpect(jsonPath("$[0].country").value("NZ"))
    }

    @Test
    fun `should get shareholders by country`() {
        val shareholders = listOf(testShareholder)
        every { shareholderService.getShareholdersByCountry(1L, "NZ") } returns shareholders

        mockMvc.perform(
            get("/v1/shareholders/company/1/by-country")
                .param("country", "NZ"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].country").value("NZ"))
    }

    @Test
    fun `should get shareholders by region`() {
        val shareholders = listOf(testShareholder)
        every { shareholderService.getShareholdersByRegion(1L, "Auckland") } returns shareholders

        mockMvc.perform(
            get("/v1/shareholders/company/1/by-region")
                .param("region", "Auckland"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].region").value("Auckland"))
    }

    @Test
    fun `should get shareholders by postcode`() {
        val shareholders = listOf(testShareholder)
        every { shareholderService.getShareholdersByPostcode(1L, "1010") } returns shareholders

        mockMvc.perform(
            get("/v1/shareholders/company/1/by-postcode")
                .param("postcode", "1010"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].postcode").value("1010"))
    }

    @Test
    fun `should search shareholders`() {
        val shareholders = listOf(testShareholder)
        every { shareholderService.searchShareholders("John") } returns shareholders

        mockMvc.perform(
            get("/v1/shareholders/search")
                .param("query", "John"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].fullName").value("John Smith"))
    }

    @Test
    fun `should search shareholders by address`() {
        val shareholders = listOf(testShareholder)
        every { shareholderService.searchShareholdersByAddress(1L, "Main St") } returns shareholders

        mockMvc.perform(
            get("/v1/shareholders/company/1/search-address")
                .param("address", "Main St"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].addressLine1").value("123 Main St"))
    }

    @Test
    fun `should get shareholder statistics`() {
        val statistics = mapOf(
            "totalShareholders" to 10,
            "individualShareholders" to 8,
            "corporateShareholders" to 2,
            "shareholdersByCountry" to mapOf("NZ" to 7, "AU" to 3),
        )
        every { shareholderService.getShareholderStatistics(1L) } returns statistics

        mockMvc.perform(get("/v1/shareholders/company/1/statistics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalShareholders").value(10))
            .andExpect(jsonPath("$.individualShareholders").value(8))
            .andExpect(jsonPath("$.corporateShareholders").value(2))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should validate shareholder data`() {
        val validation = mapOf(
            "hasRequiredFields" to true,
            "hasValidAddress" to true,
            "hasValidCountry" to true,
        )
        every { shareholderService.getShareholderById(1L) } returns testShareholder
        every { shareholderService.validateShareholderData(testShareholder) } returns validation

        mockMvc.perform(get("/v1/shareholders/1/validate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasRequiredFields").value(true))
            .andExpect(jsonPath("$.hasValidAddress").value(true))
            .andExpect(jsonPath("$.hasValidCountry").value(true))
    }

    @Test
    @WithMockUser(roles = ["REGISTRAR"])
    fun `should allow REGISTRAR role for shareholder validation`() {
        val validation = mapOf("hasRequiredFields" to true)
        every { shareholderService.getShareholderById(1L) } returns testShareholder
        every { shareholderService.validateShareholderData(testShareholder) } returns validation

        mockMvc.perform(get("/v1/shareholders/1/validate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasRequiredFields").value(true))
    }

    @Test
    @WithMockUser(roles = ["PUBLIC"])
    fun `should require ADMIN or REGISTRAR role for shareholder validation`() {
        mockMvc.perform(get("/v1/shareholders/1/validate"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should handle validation error when creating shareholder`() {
        every { shareholderService.createShareholder(any()) } throws ValidationException(
            "shareholder",
            "Shareholder with this name already exists for this company",
        )

        mockMvc.perform(
            post("/v1/shareholders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testShareholder)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("already exists")))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should handle validation error when updating shareholder`() {
        every { shareholderService.updateShareholder(1L, any()) } throws ValidationException(
            "shareholder",
            "Invalid address information",
        )

        mockMvc.perform(
            put("/v1/shareholders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testShareholder)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid address")))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should handle resource not found when deleting shareholder`() {
        every {
            shareholderService.deleteShareholder(1L)
        } throws ResourceNotFoundException("shareholder", "Shareholder not found")

        mockMvc.perform(delete("/v1/shareholders/1"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should handle empty search results`() {
        every { shareholderService.searchShareholders("nonexistent") } returns emptyList()

        mockMvc.perform(
            get("/v1/shareholders/search")
                .param("query", "nonexistent"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `should handle empty statistics`() {
        val emptyStatistics = mapOf(
            "totalShareholders" to 0,
            "individualShareholders" to 0,
            "corporateShareholders" to 0,
        )
        every { shareholderService.getShareholderStatistics(1L) } returns emptyStatistics

        mockMvc.perform(get("/v1/shareholders/company/1/statistics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalShareholders").value(0))
    }
}
