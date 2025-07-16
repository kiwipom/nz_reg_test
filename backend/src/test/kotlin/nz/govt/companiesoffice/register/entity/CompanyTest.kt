package nz.govt.companiesoffice.register.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

class CompanyTest {

    private lateinit var company: Company
    private lateinit var director: Director
    private lateinit var address: Address

    @BeforeEach
    fun setUp() {
        company = Company(
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.of(2020, 1, 1)
        )

        director = Director(
            company = company,
            fullName = "John Doe",
            residentialAddressLine1 = "123 Test Street",
            residentialCity = "Auckland",
            isNzResident = true,
            isAustralianResident = false,
            appointedDate = LocalDate.of(2020, 1, 1)
        )

        address = Address(
            company = company,
            addressType = AddressType.REGISTERED,
            addressLine1 = "456 Company Street",
            city = "Auckland",
            effectiveFrom = LocalDate.of(2020, 1, 1)
        )
    }

    @Test
    fun `should create company with required fields`() {
        assertThat(company.companyNumber).isEqualTo("12345678")
        assertThat(company.companyName).isEqualTo("Test Company Ltd")
        assertThat(company.companyType).isEqualTo(CompanyType.LTD)
        assertThat(company.incorporationDate).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(company.status).isEqualTo("ACTIVE")
        assertThat(company.version).isEqualTo(1)
    }

    @Test
    fun `should add director to company`() {
        company.addDirector(director)
        
        assertThat(company.directors).hasSize(1)
        assertThat(company.directors[0]).isEqualTo(director)
        assertThat(director.company).isEqualTo(company)
    }

    @Test
    fun `should add address to company`() {
        company.addAddress(address)
        
        assertThat(company.addresses).hasSize(1)
        assertThat(company.addresses[0]).isEqualTo(address)
        assertThat(address.company).isEqualTo(company)
    }

    @Test
    fun `should get active directors only`() {
        val activeDirector = director.copy(status = DirectorStatus.ACTIVE)
        val resignedDirector = director.copy(
            id = 2,
            fullName = "Jane Doe",
            status = DirectorStatus.RESIGNED
        )
        
        company.addDirector(activeDirector)
        company.addDirector(resignedDirector)
        
        val activeDirectors = company.getActiveDirectors()
        
        assertThat(activeDirectors).hasSize(1)
        assertThat(activeDirectors[0].status).isEqualTo(DirectorStatus.ACTIVE)
    }

    @Test
    fun `should get resident directors only`() {
        val nzResident = director.copy(isNzResident = true, isAustralianResident = false)
        val auResident = director.copy(
            id = 2,
            fullName = "Jane Doe",
            isNzResident = false,
            isAustralianResident = true
        )
        val nonResident = director.copy(
            id = 3,
            fullName = "Bob Smith",
            isNzResident = false,
            isAustralianResident = false
        )
        
        company.addDirector(nzResident)
        company.addDirector(auResident)
        company.addDirector(nonResident)
        
        val residentDirectors = company.getResidentDirectors()
        
        assertThat(residentDirectors).hasSize(2)
        assertThat(residentDirectors.map { it.fullName })
            .containsExactlyInAnyOrder("John Doe", "Jane Doe")
    }

    @Test
    fun `should get current address by type`() {
        val currentAddress = address.copy(
            effectiveFrom = LocalDate.now().minusDays(1),
            effectiveTo = null
        )
        val expiredAddress = address.copy(
            id = 2,
            effectiveFrom = LocalDate.now().minusDays(10),
            effectiveTo = LocalDate.now().minusDays(2)
        )
        
        company.addAddress(currentAddress)
        company.addAddress(expiredAddress)
        
        val current = company.getCurrentAddress(AddressType.REGISTERED)
        
        assertThat(current).isEqualTo(currentAddress)
    }

    @Test
    fun `should return null for non-existent address type`() {
        val current = company.getCurrentAddress(AddressType.SERVICE)
        
        assertThat(current).isNull()
    }

    @Test
    fun `should check if company is active`() {
        assertThat(company.isActive()).isTrue()
        
        company.status = "REMOVED"
        assertThat(company.isActive()).isFalse()
    }

    @Test
    fun `should have proper equals and hashCode`() {
        val company1 = Company(
            id = 1,
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.of(2020, 1, 1)
        )
        val company2 = Company(
            id = 1,
            companyNumber = "87654321",
            companyName = "Other Company Ltd",
            companyType = CompanyType.OVERSEAS,
            incorporationDate = LocalDate.of(2021, 1, 1)
        )
        val company3 = Company(
            id = 2,
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.of(2020, 1, 1)
        )
        
        assertThat(company1).isEqualTo(company2) // Same ID
        assertThat(company1).isNotEqualTo(company3) // Different ID
        assertThat(company1.hashCode()).isEqualTo(company2.hashCode())
        assertThat(company1.hashCode()).isNotEqualTo(company3.hashCode())
    }

    @Test
    fun `should have proper toString`() {
        val company = Company(
            id = 1,
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.of(2020, 1, 1)
        )
        
        val toString = company.toString()
        
        assertThat(toString).contains("Company")
        assertThat(toString).contains("id=1")
        assertThat(toString).contains("companyNumber='12345678'")
        assertThat(toString).contains("companyName='Test Company Ltd'")
        assertThat(toString).contains("companyType=LTD")
    }

    @Test
    fun `should test company type enum`() {
        assertThat(CompanyType.values()).containsExactly(
            CompanyType.LTD,
            CompanyType.OVERSEAS,
            CompanyType.UNLIMITED
        )
    }

    @Test
    fun `should handle nzbn field`() {
        assertThat(company.nzbn).isNull()
        
        company.nzbn = "1234567890123"
        assertThat(company.nzbn).isEqualTo("1234567890123")
    }

    @Test
    fun `should handle status changes`() {
        assertThat(company.status).isEqualTo("ACTIVE")
        
        company.status = "REMOVED"
        assertThat(company.status).isEqualTo("REMOVED")
        assertThat(company.isActive()).isFalse()
    }

    @Test
    fun `should track version for optimistic locking`() {
        assertThat(company.version).isEqualTo(1)
        
        company.version = 2
        assertThat(company.version).isEqualTo(2)
    }

    @Test
    fun `should handle empty collections`() {
        val newCompany = Company(
            companyNumber = "11111111",
            companyName = "New Company",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.now()
        )
        
        assertThat(newCompany.addresses).isEmpty()
        assertThat(newCompany.directors).isEmpty()
        assertThat(newCompany.shareholders).isEmpty()
        assertThat(newCompany.getActiveDirectors()).isEmpty()
        assertThat(newCompany.getResidentDirectors()).isEmpty()
        assertThat(newCompany.getCurrentAddress(AddressType.REGISTERED)).isNull()
    }
}