package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.entity.ShareClass
import nz.govt.companiesoffice.register.repository.CompanyRepository
import nz.govt.companiesoffice.register.repository.ShareClassRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ShareClassServiceTest {

    @Mock
    private lateinit var shareClassRepository: ShareClassRepository

    @Mock
    private lateinit var companyRepository: CompanyRepository

    @Mock
    private lateinit var auditService: AuditService

    @InjectMocks
    private lateinit var shareClassService: ShareClassService

    private lateinit var testCompany: Company
    private lateinit var testShareClass: ShareClass

    @BeforeEach
    fun setup() {
        testCompany = Company(
            id = 1L,
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.now(),
            nzbn = "9429000000000",
            status = "ACTIVE",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        testShareClass = ShareClass(
            id = 1L,
            company = testCompany,
            className = "Ordinary Shares",
            classCode = "ORDINARY",
            description = "Standard ordinary shares",
            votingRights = "ORDINARY",
            votesPerShare = 1,
            dividendRights = "ORDINARY",
            capitalDistributionRights = "ORDINARY",
            isTransferable = true,
            hasPreemptiveRights = true,
        )
    }

    @Test
    fun `should get share class by ID`() {
        // Given
        `when`(shareClassRepository.findById(1L)).thenReturn(Optional.of(testShareClass))

        // When
        val result = shareClassService.getShareClassById(1L)

        // Then
        assertNotNull(result)
        assertEquals(testShareClass.id, result!!.id)
        assertEquals(testShareClass.className, result.className)

        verify(shareClassRepository).findById(1L)
    }

    @Test
    fun `should get active share classes for company`() {
        // Given
        val shareClasses = listOf(testShareClass)
        `when`(shareClassRepository.findByCompany_IdAndIsActiveTrue(1L)).thenReturn(shareClasses)

        // When
        val result = shareClassService.getActiveShareClassesByCompany(1L)

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(testShareClass.id, result[0].id)

        verify(shareClassRepository).findByCompany_IdAndIsActiveTrue(1L)
    }

    @Test
    fun `should throw exception when company not found`() {
        // Given
        `when`(companyRepository.findById(1L)).thenReturn(Optional.empty())

        // When & Then
        assertThrows<IllegalArgumentException> {
            shareClassService.createShareClass(
                companyId = 1L,
                className = "Ordinary Shares",
                classCode = "ORDINARY",
            )
        }

        verify(companyRepository).findById(1L)
        verifyNoInteractions(shareClassRepository)
    }
}
