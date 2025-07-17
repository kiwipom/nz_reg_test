package nz.govt.companiesoffice.register.service

import io.mockk.every
import io.mockk.mockk
import nz.govt.companiesoffice.register.entity.Address
import nz.govt.companiesoffice.register.entity.AddressType
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("AddressValidationService Tests")
class AddressValidationServiceTest {

    private lateinit var nzPostAddressService: NZPostAddressService
    private lateinit var addressValidationService: AddressValidationService
    private lateinit var testCompany: Company

    @BeforeEach
    fun setUp() {
        nzPostAddressService = mockk()
        addressValidationService = AddressValidationService(nzPostAddressService)
        
        testCompany = Company(
            id = 1L,
            companyNumber = "12345678",
            companyName = "Test Company Ltd",
            companyType = CompanyType.LTD,
            incorporationDate = LocalDate.now(),
        )
    }

    @Nested
    @DisplayName("Basic Address Validation")
    inner class BasicAddressValidation {

        @Test
        fun `should validate a complete valid NZ address`() {
            // Given
            val address = createValidNZAddress()
            
            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(
                isValid = true,
                standardizedAddress = "123 Main Street, Auckland 1010",
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertTrue(result.isValid) },
                { assertTrue(result.errors.isEmpty()) },
                { assertTrue(result.suggestions.isNotEmpty()) },
            )
        }

        @Test
        fun `should reject address with missing required fields`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "", // Missing
                city = "", // Missing
                country = "NZ",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.contains("Address line 1 is required")) },
                { assertTrue(result.errors.contains("City is required")) },
            )
        }

        @Test
        fun `should validate address field lengths`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "x".repeat(256), // Too long
                addressLine2 = "y".repeat(256), // Too long
                city = "z".repeat(101), // Too long
                country = "NZ",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.contains("Address line 1 must be 255 characters or less")) },
                { assertTrue(result.errors.contains("Address line 2 must be 255 characters or less")) },
                { assertTrue(result.errors.contains("City must be 100 characters or less")) },
            )
        }

        @Test
        fun `should validate address field characters`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main St @#$%^&*()", // Invalid characters
                city = "Auckland@#$", // Invalid characters
                country = "NZ",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.contains("Address line 1 contains invalid characters")) },
                { assertTrue(result.errors.contains("City contains invalid characters")) },
            )
        }
    }

    @Nested
    @DisplayName("Email and Phone Validation")
    inner class ContactValidation {

        @Test
        fun `should validate correct email formats`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                country = "NZ",
                email = "test@example.com",
                effectiveFrom = LocalDate.now(),
            )

            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(isValid = true)

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertTrue(result.isValid)
        }

        @Test
        fun `should reject invalid email formats`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                country = "NZ",
                email = "invalid-email",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.contains("Invalid email format")) },
            )
        }

        @Test
        fun `should validate NZ phone numbers`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                country = "NZ",
                phone = "+64 9 123 4567",
                effectiveFrom = LocalDate.now(),
            )
            
            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(isValid = true)

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertTrue(result.isValid)
        }

        @Test
        fun `should reject invalid NZ phone numbers`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                country = "NZ",
                phone = "123456", // Invalid format
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.contains("Invalid New Zealand phone number format")) },
            )
        }
    }

    @Nested
    @DisplayName("NZ Address Validation")
    inner class NZAddressValidation {

        @Test
        fun `should validate NZ postcodes`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                country = "NZ",
                postcode = "1010",
                effectiveFrom = LocalDate.now(),
            )

            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(isValid = true)

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertTrue(result.isValid)
        }

        @Test
        fun `should reject invalid NZ postcodes`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                country = "NZ",
                postcode = "12345", // Invalid - too long
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.any { it.contains("New Zealand postcode must be") }) },
            )
        }

        @Test
        fun `should warn about missing postcode for NZ addresses`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                country = "NZ",
                postcode = null,
                effectiveFrom = LocalDate.now(),
            )

            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(isValid = true)

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertTrue(result.isValid) },
                { assertTrue(result.warnings.contains("Postcode is recommended for New Zealand addresses")) },
            )
        }

        @Test
        fun `should validate NZ regions`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                region = "Auckland",
                country = "NZ",
                effectiveFrom = LocalDate.now(),
            )
            
            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(isValid = true)

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertTrue(result.isValid)
        }

        @Test
        fun `should warn about invalid NZ regions`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                region = "InvalidRegion",
                country = "NZ",
                effectiveFrom = LocalDate.now(),
            )

            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(isValid = true)

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertTrue(result.isValid) },
                { assertTrue(result.warnings.any { it.contains("not a standard New Zealand region") }) },
                { assertTrue(result.suggestions.any { it.contains("Consider using one of") }) },
            )
        }
    }

    @Nested
    @DisplayName("Address Type Validation")
    inner class AddressTypeValidation {

        @Test
        fun `should reject PO Box for registered addresses`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "P.O. Box 123",
                city = "Auckland",
                country = "NZ",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.contains("Registered address cannot be a PO Box")) },
            )
        }

        @Test
        fun `should require postcode for NZ registered addresses`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                country = "NZ",
                postcode = null,
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.contains("Registered addresses in New Zealand must have a postcode")) },
            )
        }

        @Test
        fun `should require contact info for communication addresses`() {
            // Given
            val address = Address(
                company = testCompany,
                addressType = AddressType.COMMUNICATION,
                addressLine1 = "123 Main Street",
                city = "Auckland",
                country = "NZ",
                email = null,
                phone = null,
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.contains("Communication addresses must have email or phone contact information")) },
            )
        }
    }

    @Nested
    @DisplayName("Address Update Validation")
    inner class AddressUpdateValidation {

        @Test
        fun `should validate address updates`() {
            // Given
            val currentAddress = createValidNZAddress()
            val newAddress = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "456 New Street",
                city = "Auckland",
                country = "NZ",
                effectiveFrom = LocalDate.now(),
            )

            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(isValid = true)

            // When
            val result = addressValidationService.validateAddressUpdate(
                currentAddress = currentAddress,
                newAddress = newAddress,
                effectiveDate = LocalDate.now().plusDays(1),
            )

            // Then
            assertTrue(result.isValid)
        }

        @Test
        fun `should reject backdated address changes`() {
            // Given
            val currentAddress = createValidNZAddress()
            val newAddress = Address(
                company = testCompany,
                addressType = AddressType.REGISTERED,
                addressLine1 = "456 New Street",
                city = "Auckland",
                country = "NZ",
                effectiveFrom = LocalDate.now(),
            )

            // When
            val result = addressValidationService.validateAddressUpdate(
                currentAddress = currentAddress,
                newAddress = newAddress,
                effectiveDate = LocalDate.now().minusDays(1),
            )

            // Then
            assertAll(
                { assertFalse(result.isValid) },
                { assertTrue(result.errors.contains("Address changes cannot be backdated")) },
            )
        }

        @Test
        fun `should warn about identical addresses`() {
            // Given
            val currentAddress = createValidNZAddress()
            val newAddress = createValidNZAddress() // Same address

            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(isValid = true)

            // When
            val result = addressValidationService.validateAddressUpdate(
                currentAddress = currentAddress,
                newAddress = newAddress,
                effectiveDate = LocalDate.now().plusDays(1),
            )

            // Then
            assertAll(
                { assertTrue(result.isValid) },
                { assertTrue(result.warnings.contains("New address is identical to current address")) },
            )
        }
    }

    @Nested
    @DisplayName("NZ Post Integration")
    inner class NZPostIntegration {

        @Test
        fun `should handle NZ Post validation success`() {
            // Given
            val address = createValidNZAddress()
            
            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(
                isValid = true,
                standardizedAddress = "123 Main Street, Auckland 1010",
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertTrue(result.isValid) },
                { assertTrue(result.suggestions.any { it.contains("Standardized address") }) },
            )
        }

        @Test
        fun `should handle NZ Post validation failure`() {
            // Given
            val address = createValidNZAddress()
            
            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } returns NZPostAddressService.AddressValidationResult(
                isValid = false,
                suggestions = listOf("Did you mean 'Main Street'?"),
            )

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertTrue(result.isValid) }, // Still valid for our purposes
                { assertTrue(result.warnings.contains("Address could not be validated with NZ Post address database")) },
                { assertTrue(result.suggestions.contains("Did you mean 'Main Street'?")) },
            )
        }

        @Test
        fun `should handle NZ Post service exceptions`() {
            // Given
            val address = createValidNZAddress()
            
            every { 
                nzPostAddressService.validateNZAddress(any(), any(), any(), any()) 
            } throws Exception("Service unavailable")

            // When
            val result = addressValidationService.validateAddress(address)

            // Then
            assertAll(
                { assertTrue(result.isValid) }, // Still valid for our purposes
                { assertTrue(result.warnings.contains("Could not validate address with NZ Post service")) },
            )
        }
    }

    // Helper methods
    private fun createValidNZAddress(): Address {
        return Address(
            company = testCompany,
            addressType = AddressType.REGISTERED,
            addressLine1 = "123 Main Street",
            city = "Auckland",
            region = "Auckland",
            postcode = "1010",
            country = "NZ",
            effectiveFrom = LocalDate.now(),
        )
    }
}