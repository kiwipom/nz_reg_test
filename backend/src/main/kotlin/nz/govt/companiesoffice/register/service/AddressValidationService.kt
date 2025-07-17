package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.entity.Address
import nz.govt.companiesoffice.register.entity.AddressType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Enhanced address validation service for the NZ Companies Register.
 * Provides comprehensive validation for address data including format validation,
 * business rules, and NZ-specific address requirements.
 */
@Service
class AddressValidationService(
    private val nzPostAddressService: NZPostAddressService,
) {

    private val logger = LoggerFactory.getLogger(AddressValidationService::class.java)

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val suggestions: List<String> = emptyList(),
    ) {
        fun hasErrors(): Boolean = errors.isNotEmpty()
        fun hasWarnings(): Boolean = warnings.isNotEmpty()
        fun hasSuggestions(): Boolean = suggestions.isNotEmpty()
    }

    /**
     * Performs comprehensive validation of an address
     */
    fun validateAddress(address: Address): ValidationResult {
        logger.debug("Validating address ${address.id} of type ${address.addressType}")
        
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        // Basic field validation
        validateBasicFields(address, errors)
        
        // Business rule validation
        validateBusinessRules(address, errors, warnings)
        
        // Country-specific validation
        when (address.country.uppercase()) {
            "NZ" -> validateNZAddress(address, errors, warnings, suggestions)
            "AU" -> validateAUAddress(address, errors, warnings, suggestions)
            else -> validateInternationalAddress(address, errors, warnings, suggestions)
        }
        
        // Address type specific validation
        validateAddressTypeRules(address, errors, warnings)
        
        // Cross-field validation
        validateCrossFieldRules(address, errors, warnings)

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            suggestions = suggestions,
        )
    }

    /**
     * Validates address for update operations
     */
    fun validateAddressUpdate(
        currentAddress: Address,
        newAddress: Address,
        effectiveDate: LocalDate = LocalDate.now(),
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        // Validate the new address
        val addressValidation = validateAddress(newAddress)
        errors.addAll(addressValidation.errors)
        warnings.addAll(addressValidation.warnings)
        suggestions.addAll(addressValidation.suggestions)

        // Validate update-specific rules
        if (effectiveDate.isBefore(LocalDate.now())) {
            errors.add("Address changes cannot be backdated")
        }

        if (effectiveDate.isBefore(currentAddress.effectiveFrom)) {
            errors.add("New address effective date cannot be before current address effective date")
        }

        // Check if this is a meaningful change
        if (currentAddress.isSameAddress(newAddress)) {
            warnings.add("New address is identical to current address")
        }

        // Business continuity checks
        if (newAddress.addressType == AddressType.REGISTERED) {
            validateRegisteredAddressChange(currentAddress, newAddress, errors, warnings)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            suggestions = suggestions,
        )
    }

    private fun validateBasicFields(address: Address, errors: MutableList<String>) {
        // Address Line 1 validation
        if (address.addressLine1.trim().isEmpty()) {
            errors.add("Address line 1 is required")
        } else {
            if (address.addressLine1.length > 255) {
                errors.add("Address line 1 must be 255 characters or less")
            }
            if (!address.addressLine1.matches(Regex("^[a-zA-Z0-9\\s,.'-/]+$"))) {
                errors.add("Address line 1 contains invalid characters")
            }
        }

        // Address Line 2 validation
        if (address.addressLine2 != null) {
            if (address.addressLine2!!.length > 255) {
                errors.add("Address line 2 must be 255 characters or less")
            }
            if (!address.addressLine2!!.matches(Regex("^[a-zA-Z0-9\\s,.'-/]+$"))) {
                errors.add("Address line 2 contains invalid characters")
            }
        }

        // City validation
        if (address.city.trim().isEmpty()) {
            errors.add("City is required")
        } else {
            if (address.city.length > 100) {
                errors.add("City must be 100 characters or less")
            }
            if (!address.city.matches(Regex("^[a-zA-Z\\s'-]+$"))) {
                errors.add("City contains invalid characters")
            }
        }

        // Region validation
        if (address.region != null) {
            if (address.region!!.length > 100) {
                errors.add("Region must be 100 characters or less")
            }
            if (!address.region!!.matches(Regex("^[a-zA-Z\\s'-]+$"))) {
                errors.add("Region contains invalid characters")
            }
        }

        // Postcode validation (basic)
        if (address.postcode != null && address.postcode!!.length > 10) {
            errors.add("Postcode must be 10 characters or less")
        }

        // Country validation
        if (address.country.length != 2) {
            errors.add("Country must be a valid 2-letter ISO country code")
        }

        // Email validation
        if (address.email != null && !isValidEmail(address.email!!)) {
            errors.add("Invalid email format")
        }

        // Phone validation
        if (address.phone != null && !isValidPhoneNumber(address.phone!!)) {
            errors.add("Invalid phone number format")
        }
    }

    private fun validateBusinessRules(
        address: Address,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        // Date validation
        if (address.effectiveTo != null && !address.effectiveFrom.isBefore(address.effectiveTo)) {
            errors.add("Effective from date must be before effective to date")
        }

        // Future effective dates
        if (address.effectiveFrom.isAfter(LocalDate.now().plusYears(1))) {
            warnings.add("Address effective date is more than 1 year in the future")
        }

        // Contact information completeness
        if (address.addressType == AddressType.COMMUNICATION) {
            if (address.email == null && address.phone == null) {
                warnings.add("Communication addresses should have email or phone contact information")
            }
        }
    }

    private fun validateNZAddress(
        address: Address,
        errors: MutableList<String>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>,
    ) {
        // NZ postcode validation
        if (address.postcode != null) {
            if (!address.postcode!!.matches(Regex("^\\d{4}$"))) {
                errors.add("New Zealand postcode must be exactly 4 digits")
            } else {
                // Check if postcode is in valid range (1000-9999)
                val postcode = address.postcode!!.toIntOrNull()
                if (postcode == null || postcode < 1000 || postcode > 9999) {
                    errors.add("New Zealand postcode must be between 1000 and 9999")
                }
            }
        } else {
            warnings.add("Postcode is recommended for New Zealand addresses")
        }

        // NZ phone number validation
        if (address.phone != null && !isValidNZPhoneNumber(address.phone!!)) {
            errors.add("Invalid New Zealand phone number format")
        }

        // Region validation for NZ
        if (address.region != null) {
            val validNZRegions = listOf(
                "Northland", "Auckland", "Waikato", "Bay of Plenty", "Gisborne",
                "Hawke's Bay", "Taranaki", "Manawatu-Wanganui", "Wellington",
                "Tasman", "Nelson", "Marlborough", "West Coast", "Canterbury",
                "Otago", "Southland",
            )
            if (!validNZRegions.any { it.equals(address.region, ignoreCase = true) }) {
                warnings.add("Region '${address.region}' is not a standard New Zealand region")
                suggestions.add("Consider using one of: ${validNZRegions.joinToString(", ")}")
            }
        }

        // NZ-specific address validation with NZ Post service
        try {
            val nzValidationResult = nzPostAddressService.validateNZAddress(
                addressLine1 = address.addressLine1,
                addressLine2 = address.addressLine2,
                city = address.city,
                postcode = address.postcode,
            )
            if (!nzValidationResult.isValid) {
                warnings.add("Address could not be validated with NZ Post address database")
                suggestions.add("Please verify the address details are correct")
                suggestions.addAll(nzValidationResult.suggestions)
            } else if (nzValidationResult.standardizedAddress != null) {
                suggestions.add("Standardized address: ${nzValidationResult.standardizedAddress}")
            }
        } catch (e: Exception) {
            logger.warn("NZ Post address validation failed: ${e.message}")
            warnings.add("Could not validate address with NZ Post service")
        }
    }

    private fun validateAUAddress(
        address: Address,
        errors: MutableList<String>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>,
    ) {
        // Australian postcode validation
        if (address.postcode != null) {
            if (!address.postcode!!.matches(Regex("^\\d{4}$"))) {
                errors.add("Australian postcode must be exactly 4 digits")
            } else {
                val postcode = address.postcode!!.toIntOrNull()
                if (postcode == null || postcode < 1000 || postcode > 9999) {
                    errors.add("Australian postcode must be between 1000 and 9999")
                }
            }
        } else {
            warnings.add("Postcode is recommended for Australian addresses")
        }

        // Australian phone number validation
        if (address.phone != null && !isValidAUPhoneNumber(address.phone!!)) {
            errors.add("Invalid Australian phone number format")
        }

        // State/Territory validation for AU
        if (address.region != null) {
            val validAUStates = listOf("NSW", "VIC", "QLD", "WA", "SA", "TAS", "ACT", "NT")
            if (!validAUStates.any { it.equals(address.region, ignoreCase = true) }) {
                warnings.add("Region '${address.region}' is not a standard Australian state/territory")
                suggestions.add("Consider using one of: ${validAUStates.joinToString(", ")}")
            }
        }
    }

    private fun validateInternationalAddress(
        address: Address,
        errors: MutableList<String>,
        warnings: MutableList<String>,
        suggestions: MutableList<String>,
    ) {
        // For international addresses, we're more lenient but still provide guidance
        if (address.postcode == null) {
            warnings.add("Postcode/ZIP code is recommended for international addresses")
        }

        if (address.region == null) {
            warnings.add("State/Province/Region is recommended for international addresses")
        }

        // Validate country code
        if (!isValidCountryCode(address.country)) {
            errors.add("Invalid country code: ${address.country}")
        }
    }

    private fun validateAddressTypeRules(
        address: Address,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        when (address.addressType) {
            AddressType.REGISTERED -> {
                // Registered addresses have stricter requirements
                if (address.postcode == null && address.country == "NZ") {
                    errors.add("Registered addresses in New Zealand must have a postcode")
                }
                
                // Should not be a PO Box for registered address
                if (isLikelyPOBox(address.addressLine1)) {
                    errors.add("Registered address cannot be a PO Box")
                }
            }
            
            AddressType.SERVICE -> {
                // Service addresses can be PO Boxes but should have contact info
                if (address.email == null && address.phone == null) {
                    warnings.add("Service addresses should have contact information (email or phone)")
                }
            }
            
            AddressType.COMMUNICATION -> {
                // Communication addresses must have contact info
                if (address.email == null && address.phone == null) {
                    errors.add("Communication addresses must have email or phone contact information")
                }
            }
        }
    }

    private fun validateCrossFieldRules(
        address: Address,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        // Email domain validation for business addresses
        if (address.email != null && address.addressType != AddressType.COMMUNICATION) {
            val domain = address.email!!.substringAfter("@").lowercase()
            val commonPersonalDomains = listOf("gmail.com", "yahoo.com", "hotmail.com", "outlook.com")
            if (commonPersonalDomains.contains(domain)) {
                warnings.add("Consider using a business email address for ${address.addressType.name.lowercase()} addresses")
            }
        }

        // Phone and email consistency
        if (address.phone != null && address.email != null) {
            val phoneCountry = determinePhoneCountry(address.phone!!)
            if (phoneCountry != null && phoneCountry != address.country) {
                warnings.add("Phone number country ($phoneCountry) doesn't match address country (${address.country})")
            }
        }
    }

    private fun validateRegisteredAddressChange(
        currentAddress: Address,
        newAddress: Address,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        // Check for significant registered address changes
        if (currentAddress.country != newAddress.country) {
            warnings.add("Changing registered address to a different country may have legal implications")
        }

        if (currentAddress.region != newAddress.region && currentAddress.country == "NZ") {
            warnings.add("Changing registered address to a different region may affect court jurisdiction")
        }
    }

    // Helper validation methods
    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Basic international phone number validation
        val cleanPhone = phone.replace(Regex("[\\s()-]"), "")
        return cleanPhone.matches(Regex("^\\+?[1-9]\\d{6,14}$"))
    }

    private fun isValidNZPhoneNumber(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[\\s()-]"), "")
        val nzPhonePatterns = listOf(
            Regex("^\\+64[2-9]\\d{7,8}$"),      // +64 9 xxx xxxx
            Regex("^0[2-9]\\d{7,8}$"),          // 09 xxx xxxx
            Regex("^\\+6421\\d{6,7}$"),         // +64 21 xxx xxxx (mobile)
            Regex("^021\\d{6,7}$"),             // 021 xxx xxxx (mobile)
            Regex("^\\+6422\\d{6,7}$"),         // +64 22 xxx xxxx (mobile)
            Regex("^022\\d{6,7}$"),             // 022 xxx xxxx (mobile)
            Regex("^\\+6427\\d{6,7}$"),         // +64 27 xxx xxxx (mobile)
            Regex("^027\\d{6,7}$"),             // 027 xxx xxxx (mobile)
        )
        return nzPhonePatterns.any { it.matches(cleanPhone) }
    }

    private fun isValidAUPhoneNumber(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[\\s()-]"), "")
        val auPhonePatterns = listOf(
            Regex("^\\+61[2-8]\\d{8}$"),        // +61 2 xxxx xxxx (landline)
            Regex("^0[2-8]\\d{8}$"),            // 02 xxxx xxxx (landline)
            Regex("^\\+614\\d{8}$"),            // +61 4xx xxx xxx (mobile)
            Regex("^04\\d{8}$"),                // 04xx xxx xxx (mobile)
        )
        return auPhonePatterns.any { it.matches(cleanPhone) }
    }

    private fun isValidCountryCode(countryCode: String): Boolean {
        // This would ideally use a comprehensive list of ISO 3166-1 alpha-2 codes
        val validCodes = setOf(
            "AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR", "AS", "AT", "AU", "AW", "AX", "AZ",
            "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BL", "BM", "BN", "BO", "BQ", "BR", "BS",
            "BT", "BV", "BW", "BY", "BZ", "CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN",
            "CO", "CR", "CU", "CV", "CW", "CX", "CY", "CZ", "DE", "DJ", "DK", "DM", "DO", "DZ", "EC", "EE",
            "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK", "FM", "FO", "FR", "GA", "GB", "GD", "GE", "GF",
            "GG", "GH", "GI", "GL", "GM", "GN", "GP", "GQ", "GR", "GS", "GT", "GU", "GW", "GY", "HK", "HM",
            "HN", "HR", "HT", "HU", "ID", "IE", "IL", "IM", "IN", "IO", "IQ", "IR", "IS", "IT", "JE", "JM",
            "JO", "JP", "KE", "KG", "KH", "KI", "KM", "KN", "KP", "KR", "KW", "KY", "KZ", "LA", "LB", "LC",
            "LI", "LK", "LR", "LS", "LT", "LU", "LV", "LY", "MA", "MC", "MD", "ME", "MF", "MG", "MH", "MK",
            "ML", "MM", "MN", "MO", "MP", "MQ", "MR", "MS", "MT", "MU", "MV", "MW", "MX", "MY", "MZ", "NA",
            "NC", "NE", "NF", "NG", "NI", "NL", "NO", "NP", "NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG",
            "PH", "PK", "PL", "PM", "PN", "PR", "PS", "PT", "PW", "PY", "QA", "RE", "RO", "RS", "RU", "RW",
            "SA", "SB", "SC", "SD", "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "SR", "SS",
            "ST", "SV", "SX", "SY", "SZ", "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TL", "TM", "TN", "TO",
            "TR", "TT", "TV", "TW", "TZ", "UA", "UG", "UM", "US", "UY", "UZ", "VA", "VC", "VE", "VG", "VI",
            "VN", "VU", "WF", "WS", "YE", "YT", "ZA", "ZM", "ZW",
        )
        return validCodes.contains(countryCode.uppercase())
    }

    private fun isLikelyPOBox(addressLine: String): Boolean {
        val poBoxPatterns = listOf(
            Regex(".*\\bP\\.?O\\.?\\s*BOX\\b.*", RegexOption.IGNORE_CASE),
            Regex(".*\\bPOST\\s*OFFICE\\s*BOX\\b.*", RegexOption.IGNORE_CASE),
            Regex(".*\\bPOB\\b.*", RegexOption.IGNORE_CASE),
            Regex(".*\\bPRIVATE\\s*BAG\\b.*", RegexOption.IGNORE_CASE),
        )
        return poBoxPatterns.any { it.matches(addressLine) }
    }

    private fun determinePhoneCountry(phone: String): String? {
        val cleanPhone = phone.replace(Regex("[\\s()-]"), "")
        return when {
            cleanPhone.startsWith("+64") || (cleanPhone.startsWith("0") && cleanPhone.length <= 10) -> "NZ"
            cleanPhone.startsWith("+61") -> "AU"
            cleanPhone.startsWith("+1") -> "US"
            cleanPhone.startsWith("+44") -> "GB"
            else -> null
        }
    }
}