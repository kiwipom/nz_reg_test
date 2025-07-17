package nz.govt.companiesoffice.register.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import nz.govt.companiesoffice.register.config.SecurityConfig
import nz.govt.companiesoffice.register.config.TestSecurityConfig
import nz.govt.companiesoffice.register.dto.CancellationRequest
import nz.govt.companiesoffice.register.dto.PaymentUpdateRequest
import nz.govt.companiesoffice.register.dto.ShareAllocationRequest
import nz.govt.companiesoffice.register.dto.ShareTransferRequest
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.entity.ShareAllocation
import nz.govt.companiesoffice.register.entity.Shareholder
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.service.ShareAllocationService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(ShareAllocationController::class)
@Import(SecurityConfig::class, TestSecurityConfig::class)
class ShareAllocationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var shareAllocationService: ShareAllocationService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var testCompany: Company
    private lateinit var testShareholder: Shareholder
    private lateinit var testShareAllocation: ShareAllocation

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
            city = "Auckland",
            country = "NZ",
            isIndividual = true,
        )

        testShareAllocation = ShareAllocation(
            id = 1L,
            company = testCompany,
            shareholder = testShareholder,
            shareClass = "Ordinary",
            numberOfShares = 1000,
            nominalValue = BigDecimal("1.00"),
            amountPaid = BigDecimal("1000.00"),
            allocationDate = LocalDate.now(),
            certificateNumber = "CERT001",
            status = "ACTIVE",
            isFullyPaid = true,
        )
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should allocate shares successfully`() {
        val request = ShareAllocationRequest(
            companyId = 1L,
            shareholderId = 1L,
            shareClass = "Ordinary",
            numberOfShares = 1000,
            nominalValue = BigDecimal("1.00"),
            amountPaid = BigDecimal("1000.00"),
            allocationDate = LocalDate.now(),
            certificateNumber = "CERT001",
            restrictions = null,
        )

        every {
            shareAllocationService.allocateShares(
                companyId = 1L,
                shareholderId = 1L,
                shareClass = "Ordinary",
                numberOfShares = 1000,
                nominalValue = BigDecimal("1.00"),
                amountPaid = BigDecimal("1000.00"),
                allocationDate = any(),
                certificateNumber = "CERT001",
                restrictions = null,
            )
        } returns testShareAllocation

        mockMvc.perform(
            post("/v1/share-allocations/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.shareClass").value("Ordinary"))
            .andExpect(jsonPath("$.numberOfShares").value(1000))
            .andExpect(jsonPath("$.nominalValue").value(1.00))
            .andExpect(jsonPath("$.amountPaid").value(1000.00))
            .andExpect(jsonPath("$.certificateNumber").value("CERT001"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.isFullyPaid").value(true))

        verify {
            shareAllocationService.allocateShares(
                companyId = 1L,
                shareholderId = 1L,
                shareClass = "Ordinary",
                numberOfShares = 1000,
                nominalValue = BigDecimal("1.00"),
                amountPaid = BigDecimal("1000.00"),
                allocationDate = any(),
                certificateNumber = "CERT001",
                restrictions = null,
            )
        }
    }

    @Test
    @WithMockUser(roles = ["REGISTRAR"])
    fun `should allow REGISTRAR role for share allocation`() {
        val request = ShareAllocationRequest(
            companyId = 1L,
            shareholderId = 1L,
            shareClass = "Ordinary",
            numberOfShares = 1000,
            nominalValue = BigDecimal("1.00"),
        )

        every {
            shareAllocationService.allocateShares(
                companyId = 1L,
                shareholderId = 1L,
                shareClass = "Ordinary",
                numberOfShares = 1000,
                nominalValue = BigDecimal("1.00"),
                amountPaid = BigDecimal.ZERO,
                allocationDate = any(),
                certificateNumber = null,
                restrictions = null,
            )
        } returns testShareAllocation

        mockMvc.perform(
            post("/v1/share-allocations/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.shareClass").value("Ordinary"))
    }

    @Test
    fun `should require authentication for share allocation`() {
        val request = ShareAllocationRequest(
            companyId = 1L,
            shareholderId = 1L,
            shareClass = "Ordinary",
            numberOfShares = 1000,
            nominalValue = BigDecimal("1.00"),
        )

        mockMvc.perform(
            post("/v1/share-allocations/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["PUBLIC"])
    fun `should require ADMIN or REGISTRAR role for share allocation`() {
        val request = ShareAllocationRequest(
            companyId = 1L,
            shareholderId = 1L,
            shareClass = "Ordinary",
            numberOfShares = 1000,
            nominalValue = BigDecimal("1.00"),
        )

        mockMvc.perform(
            post("/v1/share-allocations/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should transfer shares successfully`() {
        val request = ShareTransferRequest(
            toShareholderId = 2L,
            transferDate = LocalDate.now(),
            certificateNumber = "CERT002",
        )

        val transferredShareholder = Shareholder(
            id = 2L,
            company = testCompany,
            fullName = "Jane Smith",
            addressLine1 = "456 Oak Ave",
            city = "Wellington",
            country = "NZ",
            isIndividual = true,
        )
        val transferredAllocation = ShareAllocation(
            id = 2L,
            company = testCompany,
            shareholder = transferredShareholder,
            shareClass = "Ordinary",
            numberOfShares = 1000,
            nominalValue = BigDecimal("1.00"),
            amountPaid = BigDecimal("1000.00"),
            allocationDate = LocalDate.now(),
            certificateNumber = "CERT002",
            status = "ACTIVE",
            isFullyPaid = true,
        )

        every {
            shareAllocationService.transferShares(
                allocationId = 1L,
                toShareholderId = 2L,
                transferDate = any(),
                certificateNumber = "CERT002",
            )
        } returns transferredAllocation

        mockMvc.perform(
            post("/v1/share-allocations/1/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.certificateNumber").value("CERT002"))
            .andExpect(jsonPath("$.shareholder.fullName").value("Jane Smith"))

        verify {
            shareAllocationService.transferShares(
                allocationId = 1L,
                toShareholderId = 2L,
                transferDate = any(),
                certificateNumber = "CERT002",
            )
        }
    }

    @Test
    @WithMockUser(roles = ["REGISTRAR"])
    fun `should allow REGISTRAR role for share transfer`() {
        val request = ShareTransferRequest(toShareholderId = 2L)

        every {
            shareAllocationService.transferShares(
                allocationId = 1L,
                toShareholderId = 2L,
                transferDate = any(),
                certificateNumber = null,
            )
        } returns testShareAllocation

        mockMvc.perform(
            post("/v1/share-allocations/1/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(roles = ["PUBLIC"])
    fun `should require ADMIN or REGISTRAR role for share transfer`() {
        val request = ShareTransferRequest(toShareholderId = 2L)

        mockMvc.perform(
            post("/v1/share-allocations/1/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should update payment successfully`() {
        val request = PaymentUpdateRequest(additionalPayment = BigDecimal("500.00"))

        val updatedAllocation = ShareAllocation(
            id = 1L,
            company = testCompany,
            shareholder = testShareholder,
            shareClass = "Ordinary",
            numberOfShares = 1000,
            nominalValue = BigDecimal("1.00"),
            amountPaid = BigDecimal("1500.00"),
            allocationDate = LocalDate.now(),
            certificateNumber = "CERT001",
            status = "ACTIVE",
            isFullyPaid = false,
        )

        every {
            shareAllocationService.updatePayment(
                allocationId = 1L,
                additionalPayment = BigDecimal("500.00"),
            )
        } returns updatedAllocation

        mockMvc.perform(
            post("/v1/share-allocations/1/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amountPaid").value(1500.00))
            .andExpect(jsonPath("$.isFullyPaid").value(false))

        verify {
            shareAllocationService.updatePayment(
                allocationId = 1L,
                additionalPayment = BigDecimal("500.00"),
            )
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should cancel allocation successfully`() {
        val request = CancellationRequest(reason = "Duplicate allocation")

        val cancelledAllocation = ShareAllocation(
            id = 1L,
            company = testCompany,
            shareholder = testShareholder,
            shareClass = "Ordinary",
            numberOfShares = 1000,
            nominalValue = BigDecimal("1.00"),
            amountPaid = BigDecimal("1000.00"),
            allocationDate = LocalDate.now(),
            certificateNumber = "CERT001",
            status = "CANCELLED",
            isFullyPaid = true,
        )

        every {
            shareAllocationService.cancelAllocation(
                allocationId = 1L,
                reason = "Duplicate allocation",
            )
        } returns cancelledAllocation

        mockMvc.perform(
            post("/v1/share-allocations/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))

        verify {
            shareAllocationService.cancelAllocation(
                allocationId = 1L,
                reason = "Duplicate allocation",
            )
        }
    }

    @Test
    @WithMockUser(roles = ["REGISTRAR"])
    fun `should require ADMIN role for cancellation`() {
        val request = CancellationRequest(reason = "Test")

        mockMvc.perform(
            post("/v1/share-allocations/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should get share allocation by id`() {
        every { shareAllocationService.getShareAllocationById(1L) } returns testShareAllocation

        mockMvc.perform(
            get("/v1/share-allocations/1")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.shareClass").value("Ordinary"))
            .andExpect(jsonPath("$.numberOfShares").value(1000))
    }

    @Test
    fun `should allow public access to get share allocation`() {
        every { shareAllocationService.getShareAllocationById(1L) } returns testShareAllocation

        mockMvc.perform(get("/v1/share-allocations/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shareClass").value("Ordinary"))
    }

    @Test
    fun `should return 404 when share allocation not found`() {
        every {
            shareAllocationService.getShareAllocationById(1L)
        } throws ResourceNotFoundException("shareAllocation", "Share allocation not found")

        mockMvc.perform(get("/v1/share-allocations/1"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should get company allocations`() {
        val allocations = listOf(testShareAllocation)
        every { shareAllocationService.getActiveAllocationsByCompany(1L) } returns allocations

        mockMvc.perform(get("/v1/share-allocations/company/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].shareClass").value("Ordinary"))
    }

    @Test
    fun `should get shareholder allocations`() {
        val allocations = listOf(testShareAllocation)
        every { shareAllocationService.getActiveAllocationsByShareholder(1L) } returns allocations

        mockMvc.perform(get("/v1/share-allocations/shareholder/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].numberOfShares").value(1000))
    }

    @Test
    fun `should get allocations by share class`() {
        val allocations = listOf(testShareAllocation)
        every { shareAllocationService.getActiveAllocationsByShareClass(1L, "Ordinary") } returns allocations

        mockMvc.perform(get("/v1/share-allocations/company/1/share-class/Ordinary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].shareClass").value("Ordinary"))
    }

    @Test
    fun `should get company share statistics`() {
        val statistics = mapOf(
            "totalShares" to 10000,
            "totalValue" to BigDecimal("10000.00"),
            "sharesByClass" to mapOf("Ordinary" to 8000, "Preference" to 2000),
            "fullyPaidShares" to 8000,
            "partiallyPaidShares" to 2000,
        )
        every { shareAllocationService.getCompanyShareStatistics(1L) } returns statistics

        mockMvc.perform(get("/v1/share-allocations/company/1/statistics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalShares").value(10000))
            .andExpect(jsonPath("$.totalValue").value(10000.00))
            .andExpect(jsonPath("$.fullyPaidShares").value(8000))
    }

    @Test
    fun `should get shareholder portfolio`() {
        val portfolio = mapOf(
            "totalShares" to 1000,
            "totalValue" to BigDecimal("1000.00"),
            "totalPaid" to BigDecimal("1000.00"),
            "unpaidAmount" to BigDecimal("0.00"),
            "companies" to listOf(
                mapOf(
                    "companyId" to 1L,
                    "companyName" to "Test Company Ltd",
                    "shares" to 1000,
                    "value" to BigDecimal("1000.00"),
                ),
            ),
        )
        every { shareAllocationService.getShareholderPortfolio(1L) } returns portfolio

        mockMvc.perform(get("/v1/share-allocations/shareholder/1/portfolio"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalShares").value(1000))
            .andExpect(jsonPath("$.totalValue").value(1000.00))
            .andExpect(jsonPath("$.unpaidAmount").value(0.00))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should handle validation error when allocating shares`() {
        val request = ShareAllocationRequest(
            companyId = 1L,
            shareholderId = 1L,
            shareClass = "Ordinary",
            numberOfShares = 1000,
            nominalValue = BigDecimal("1.00"),
        )

        every {
            shareAllocationService.allocateShares(
                companyId = 1L,
                shareholderId = 1L,
                shareClass = "Ordinary",
                numberOfShares = 1000,
                nominalValue = BigDecimal("1.00"),
                amountPaid = BigDecimal.ZERO,
                allocationDate = any(),
                certificateNumber = null,
                restrictions = null,
            )
        } throws ValidationException("allocation", "Insufficient authorized shares")

        mockMvc.perform(
            post("/v1/share-allocations/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Insufficient authorized shares")))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should handle validation error when transferring shares`() {
        val request = ShareTransferRequest(toShareholderId = 2L)

        every {
            shareAllocationService.transferShares(
                allocationId = 1L,
                toShareholderId = 2L,
                transferDate = any(),
                certificateNumber = null,
            )
        } throws ValidationException("transfer", "Cannot transfer to same shareholder")

        mockMvc.perform(
            post("/v1/share-allocations/1/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cannot transfer to same shareholder")))
    }

    @Test
    fun `should handle empty allocation list`() {
        every { shareAllocationService.getActiveAllocationsByCompany(1L) } returns emptyList()

        mockMvc.perform(get("/v1/share-allocations/company/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `should handle empty statistics`() {
        val emptyStatistics = mapOf(
            "totalShares" to 0,
            "totalValue" to BigDecimal.ZERO,
            "sharesByClass" to emptyMap<String, Int>(),
        )
        every { shareAllocationService.getCompanyShareStatistics(1L) } returns emptyStatistics

        mockMvc.perform(get("/v1/share-allocations/company/1/statistics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalShares").value(0))
    }
}
