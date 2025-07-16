package nz.govt.companiesoffice.register.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NZPostAddressService {

    private val logger = LoggerFactory.getLogger(NZPostAddressService::class.java)

    data class AddressValidationResult(
        val isValid: Boolean,
        val standardizedAddress: String? = null,
        val suggestions: List<String> = emptyList(),
        val errors: List<String> = emptyList(),
    )

    data class PostcodeValidationResult(
        val isValid: Boolean,
        val city: String? = null,
        val region: String? = null,
        val suggestions: List<String> = emptyList(),
    )

    /**
     * Validates a New Zealand address against NZ Post standards
     * Note: This is a placeholder implementation for future NZ Post API integration
     */
    fun validateNZAddress(
        addressLine1: String,
        addressLine2: String? = null,
        city: String,
        postcode: String?,
    ): AddressValidationResult {
        logger.debug("Validating NZ address: $addressLine1, $city, $postcode")

        val errors = mutableListOf<String>()

        // Basic validation for now (placeholder for real NZ Post API)
        if (postcode != null && !postcode.matches(Regex("^\\d{4}$"))) {
            errors.add("New Zealand postcode must be 4 digits")
        }

        // Placeholder: In real implementation, this would call NZ Post API
        val isValid = errors.isEmpty()
        val standardizedAddress = if (isValid) {
            listOfNotNull(addressLine1, addressLine2, city, postcode).joinToString(", ")
        } else {
            null
        }

        return AddressValidationResult(
            isValid = isValid,
            standardizedAddress = standardizedAddress,
            errors = errors,
        )
    }

    /**
     * Validates a New Zealand postcode and returns city/region information
     * Note: This is a placeholder implementation for future NZ Post API integration
     */
    fun validatePostcode(postcode: String): PostcodeValidationResult {
        logger.debug("Validating NZ postcode: $postcode")

        if (!postcode.matches(Regex("^\\d{4}$"))) {
            return PostcodeValidationResult(
                isValid = false,
                suggestions = listOf("Postcode must be 4 digits"),
            )
        }

        // Placeholder: In real implementation, this would call NZ Post API
        // For now, return some known examples
        val cityRegionMap = mapOf(
            "1010" to Pair("Auckland", "Auckland"),
            "6011" to Pair("Wellington", "Wellington"),
            "8011" to Pair("Christchurch", "Canterbury"),
            "9016" to Pair("Dunedin", "Otago"),
            "3110" to Pair("Hamilton", "Waikato"),
            "4410" to Pair("Palmerston North", "Manawatu-Wanganui"),
            "7010" to Pair("Nelson", "Nelson"),
            "9810" to Pair("Invercargill", "Southland"),
        )

        val cityRegion = cityRegionMap[postcode]

        return PostcodeValidationResult(
            isValid = cityRegion != null,
            city = cityRegion?.first,
            region = cityRegion?.second,
            suggestions = if (cityRegion == null) {
                listOf("Postcode not found in database")
            } else {
                emptyList()
            },
        )
    }

    /**
     * Suggests address completions based on partial input
     * Note: This is a placeholder implementation for future NZ Post API integration
     */
    fun suggestAddresses(partialAddress: String): List<String> {
        logger.debug("Suggesting addresses for: $partialAddress")

        // Placeholder: In real implementation, this would call NZ Post API
        val suggestions = mutableListOf<String>()

        // Some basic suggestions based on common NZ addresses
        if (partialAddress.contains("Queen", ignoreCase = true)) {
            suggestions.addAll(
                listOf(
                    "1 Queen Street, Auckland 1010",
                    "Queen Street, Auckland 1010",
                    "Queens Drive, Auckland 1051",
                ),
            )
        }

        if (partialAddress.contains("Lambton", ignoreCase = true)) {
            suggestions.addAll(
                listOf(
                    "1 Lambton Quay, Wellington 6011",
                    "Lambton Quay, Wellington 6011",
                ),
            )
        }

        return suggestions.take(5) // Limit to 5 suggestions
    }

    /**
     * Gets a list of valid NZ cities
     * Note: This is a placeholder implementation for future NZ Post API integration
     */
    fun getValidCities(): List<String> {
        // Placeholder: In real implementation, this would call NZ Post API
        return listOf(
            "Auckland",
            "Wellington",
            "Christchurch",
            "Dunedin",
            "Hamilton",
            "Palmerston North",
            "Nelson",
            "Invercargill",
            "Rotorua",
            "Napier",
            "Hastings",
            "Gisborne",
            "New Plymouth",
            "Whangarei",
            "Tauranga",
        ).sorted()
    }

    /**
     * Gets a list of valid NZ regions
     * Note: This is a placeholder implementation for future NZ Post API integration
     */
    fun getValidRegions(): List<String> {
        // Placeholder: In real implementation, this would call NZ Post API
        return listOf(
            "Auckland",
            "Bay of Plenty",
            "Canterbury",
            "Gisborne",
            "Hawke's Bay",
            "Manawatu-Wanganui",
            "Marlborough",
            "Nelson",
            "Northland",
            "Otago",
            "Southland",
            "Taranaki",
            "Tasman",
            "Waikato",
            "Wellington",
            "West Coast",
        ).sorted()
    }
}
