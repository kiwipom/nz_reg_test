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
        val confidence: Double = 0.0,
        val addressComponents: AddressComponents? = null,
    )

    data class PostcodeValidationResult(
        val isValid: Boolean,
        val city: String? = null,
        val region: String? = null,
        val suggestions: List<String> = emptyList(),
        val alternativePostcodes: List<String> = emptyList(),
    )

    data class AddressComponents(
        val streetNumber: String? = null,
        val streetName: String? = null,
        val streetType: String? = null,
        val suburb: String? = null,
        val city: String,
        val region: String? = null,
        val postcode: String? = null,
        val country: String = "NZ",
    )

    data class AddressStandardizationResult(
        val originalAddress: String,
        val standardizedAddress: String,
        val components: AddressComponents,
        val confidence: Double,
        val suggestions: List<String> = emptyList(),
    )

    data class AddressSuggestion(
        val address: String,
        val components: AddressComponents,
        val score: Double,
        val type: SuggestionType,
    )

    enum class SuggestionType {
        EXACT_MATCH,
        CLOSE_MATCH,
        PARTIAL_MATCH,
        ALTERNATIVE_SPELLING,
        SIMILAR_ADDRESS,
    }

    /**
     * Validates a New Zealand address against NZ Post standards
     * Enhanced with comprehensive validation and standardization
     */
    fun validateNZAddress(
        addressLine1: String,
        addressLine2: String? = null,
        city: String,
        postcode: String?,
    ): AddressValidationResult {
        logger.debug("Validating NZ address: $addressLine1, $city, $postcode")

        val errors = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var confidence = 0.0

        // Enhanced validation
        if (postcode != null && !postcode.matches(Regex("^\\d{4}$"))) {
            errors.add("New Zealand postcode must be 4 digits")
        }

        // Validate city against known NZ cities
        val validCities = getValidCities()
        val cityMatch = findBestCityMatch(city, validCities)
        if (cityMatch == null) {
            errors.add("Unknown city: $city")
            suggestions.addAll(getSimilarCities(city))
        } else {
            confidence += 0.3
            if (cityMatch != city) {
                suggestions.add("Did you mean '$cityMatch'?")
            }
        }

        // Validate postcode-city consistency
        if (postcode != null && cityMatch != null) {
            val postcodeResult = validatePostcode(postcode)
            if (postcodeResult.isValid && postcodeResult.city != null) {
                if (!postcodeResult.city.equals(cityMatch, ignoreCase = true)) {
                    errors.add("Postcode $postcode does not match city $cityMatch")
                    suggestions.add("Postcode $postcode is for ${postcodeResult.city}")
                }
            }
        }

        // Parse and validate address components
        val components = parseAddressComponents(addressLine1, addressLine2, cityMatch ?: city, postcode)
        if (components != null) {
            confidence += 0.4
        }

        // Standardize address format
        val standardizedAddress = if (errors.isEmpty()) {
            standardizeAddress(
                components ?: createBasicComponents(addressLine1, addressLine2, cityMatch ?: city, postcode),
            )
        } else {
            null
        }

        // Calculate final confidence
        if (errors.isEmpty()) {
            confidence += 0.3
            if (postcode != null) confidence += 0.2
            if (addressLine2 == null) confidence += 0.1 // Simpler addresses are more confident
        }

        return AddressValidationResult(
            isValid = errors.isEmpty(),
            standardizedAddress = standardizedAddress,
            errors = errors,
            suggestions = suggestions,
            confidence = confidence.coerceIn(0.0, 1.0),
            addressComponents = components,
        )
    }

    /**
     * Validates a New Zealand postcode and returns city/region information
     * Enhanced with comprehensive postcode database and suggestions
     */
    fun validatePostcode(postcode: String): PostcodeValidationResult {
        logger.debug("Validating NZ postcode: $postcode")

        if (!postcode.matches(Regex("^\\d{4}$"))) {
            return PostcodeValidationResult(
                isValid = false,
                suggestions = listOf("Postcode must be 4 digits"),
                alternativePostcodes = getSimilarPostcodes(postcode),
            )
        }

        // Enhanced postcode database with more comprehensive coverage
        val cityRegionMap = getPostcodeCityRegionMap()
        val cityRegion = cityRegionMap[postcode]

        val suggestions = mutableListOf<String>()
        val alternatives = mutableListOf<String>()

        if (cityRegion == null) {
            // Find similar postcodes
            alternatives.addAll(findSimilarPostcodes(postcode))
            if (alternatives.isNotEmpty()) {
                suggestions.add("Similar postcodes found: ${alternatives.take(3).joinToString(", ")}")
            } else {
                suggestions.add("Postcode not found in NZ Post database")
            }
        }

        return PostcodeValidationResult(
            isValid = cityRegion != null,
            city = cityRegion?.first,
            region = cityRegion?.second,
            suggestions = suggestions,
            alternativePostcodes = alternatives,
        )
    }

    /**
     * Suggests address completions based on partial input
     * Enhanced with intelligent address matching and scoring
     */
    fun suggestAddresses(partialAddress: String): List<AddressSuggestion> {
        logger.debug("Suggesting addresses for: $partialAddress")

        val suggestions = mutableListOf<AddressSuggestion>()
        val input = partialAddress.lowercase().trim()

        // Get comprehensive address database
        val addressDatabase = getKnownAddressDatabase()

        // Find matches using different strategies
        suggestions.addAll(findExactMatches(input, addressDatabase))
        suggestions.addAll(findPartialMatches(input, addressDatabase))
        suggestions.addAll(findSimilarAddresses(input, addressDatabase))

        // Sort by score and type preference
        return suggestions
            .sortedWith(
                compareByDescending<AddressSuggestion> { it.type.ordinal }
                    .thenByDescending { it.score },
            )
            .take(10)
    }

    /**
     * Legacy method for backward compatibility
     */
    fun suggestAddresses(partialAddress: String, maxResults: Int = 5): List<String> {
        return suggestAddresses(partialAddress)
            .take(maxResults)
            .map { it.address }
    }

    /**
     * Standardizes an address using NZ Post formatting rules
     */
    fun standardizeAddress(address: String): AddressStandardizationResult {
        logger.debug("Standardizing address: $address")

        val components = parseAddressString(address)
        val standardized = standardizeAddress(components)
        val confidence = calculateAddressConfidence(components)

        return AddressStandardizationResult(
            originalAddress = address,
            standardizedAddress = standardized,
            components = components,
            confidence = confidence,
        )
    }

    /**
     * Gets a list of valid NZ cities (enhanced with more comprehensive list)
     */
    fun getValidCities(): List<String> {
        return listOf(
            "Albany", "Alexandra", "Ashburton", "Auckland", "Blenheim", "Cambridge",
            "Christchurch", "Cromwell", "Dunedin", "Feilding", "Gisborne", "Gore",
            "Greymouth", "Hamilton", "Hastings", "Hawera", "Hokitika", "Huntly",
            "Invercargill", "Kaikohe", "Kaitaia", "Kapiti", "Kawerau", "Kerikeri",
            "Levin", "Lower Hutt", "Manukau", "Marlborough", "Masterton", "Matamata",
            "Motueka", "Napier", "Nelson", "New Plymouth", "North Shore", "Oamaru",
            "Ohakune", "Opotiki", "Otorohanga", "Pahiatua", "Paihia", "Palmerston North",
            "Papakura", "Paraparaumu", "Picton", "Porirua", "Pukekohe", "Queenstown",
            "Rangiora", "Richmond", "Rolleston", "Rotorua", "Stratford", "Tairua",
            "Takaka", "Taumarunui", "Taupo", "Tauranga", "Te Anau", "Te Awamutu",
            "Te Kuiti", "Thames", "Timaru", "Tokoroa", "Turangi", "Upper Hutt",
            "Waihi", "Waikanae", "Waikato", "Waimate", "Waiuku", "Waitakere",
            "Wanaka", "Wanganui", "Wellington", "Westport", "Whakatane", "Whangarei",
            "Whitianga", "Winton",
        ).sorted()
    }

    /**
     * Gets a list of valid NZ regions
     */
    fun getValidRegions(): List<String> {
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

    // Private helper methods for enhanced functionality

    private fun parseAddressComponents(
        addressLine1: String,
        addressLine2: String?,
        city: String,
        postcode: String?,
    ): AddressComponents? {
        return try {
            val streetParts = parseStreetAddress(addressLine1)
            val region = getRegionForCity(city)

            AddressComponents(
                streetNumber = streetParts["number"],
                streetName = streetParts["name"],
                streetType = streetParts["type"],
                suburb = addressLine2,
                city = city,
                region = region,
                postcode = postcode,
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse address components: ${e.message}")
            null
        }
    }

    private fun createBasicComponents(
        addressLine1: String,
        addressLine2: String?,
        city: String,
        postcode: String?,
    ): AddressComponents {
        return AddressComponents(
            streetName = addressLine1,
            suburb = addressLine2,
            city = city,
            postcode = postcode,
        )
    }

    private fun standardizeAddress(components: AddressComponents): String {
        val parts = mutableListOf<String>()

        if (components.streetNumber != null && components.streetName != null) {
            val streetPart = "${components.streetNumber} ${components.streetName}"
            if (components.streetType != null) {
                parts.add("$streetPart ${components.streetType}")
            } else {
                parts.add(streetPart)
            }
        } else if (components.streetName != null) {
            parts.add(components.streetName)
        }

        components.suburb?.let { parts.add(it) }
        parts.add(components.city)
        components.postcode?.let { parts.add(it) }

        return parts.joinToString(", ")
    }

    private fun parseAddressString(address: String): AddressComponents {
        val parts = address.split(",").map { it.trim() }
        val postcode = parts.lastOrNull()?.takeIf { it.matches(Regex("^\\d{4}$")) }
        val city = if (postcode != null) parts[parts.size - 2] else parts.last()
        val streetAddress = parts.first()

        return AddressComponents(
            streetName = streetAddress,
            city = city,
            postcode = postcode,
        )
    }

    private fun calculateAddressConfidence(components: AddressComponents): Double {
        var confidence = 0.0

        if (components.streetNumber != null) confidence += 0.2
        if (components.streetName != null) confidence += 0.3
        if (components.postcode != null) confidence += 0.2
        if (getValidCities().contains(components.city)) confidence += 0.2
        if (components.region != null && getValidRegions().contains(components.region)) confidence += 0.1

        return confidence.coerceIn(0.0, 1.0)
    }

    private fun parseStreetAddress(streetAddress: String): Map<String, String> {
        val parts = streetAddress.trim().split("\\s+".toRegex())
        val result = mutableMapOf<String, String>()

        // Try to identify street number (first part if numeric)
        if (parts.isNotEmpty() && parts[0].matches(Regex("^\\d+[A-Za-z]?$"))) {
            result["number"] = parts[0]
            if (parts.size > 1) {
                result["name"] = parts.subList(1, parts.size).joinToString(" ")
            }
        } else {
            result["name"] = streetAddress
        }

        return result
    }

    private fun getRegionForCity(city: String): String? {
        val cityRegionMap = mapOf(
            "Auckland" to "Auckland",
            "Wellington" to "Wellington",
            "Christchurch" to "Canterbury",
            "Hamilton" to "Waikato",
            "Dunedin" to "Otago",
            "Tauranga" to "Bay of Plenty",
            "Palmerston North" to "Manawatu-Wanganui",
            "Nelson" to "Nelson",
            "Invercargill" to "Southland",
            "Rotorua" to "Bay of Plenty",
            "Napier" to "Hawke's Bay",
            "New Plymouth" to "Taranaki",
            "Whangarei" to "Northland",
            "Gisborne" to "Gisborne",
        )

        return cityRegionMap[city]
    }

    private fun findBestCityMatch(city: String, validCities: List<String>): String? {
        val cityLower = city.lowercase()

        // Exact match
        validCities.forEach { validCity ->
            if (validCity.lowercase() == cityLower) {
                return validCity
            }
        }

        // Partial match
        validCities.forEach { validCity ->
            if (validCity.lowercase().contains(cityLower) || cityLower.contains(validCity.lowercase())) {
                return validCity
            }
        }

        return null
    }

    private fun getSimilarCities(city: String): List<String> {
        val cityLower = city.lowercase()
        return getValidCities().filter { validCity ->
            val distance = calculateLevenshteinDistance(cityLower, validCity.lowercase())
            distance <= 3 && validCity.length >= city.length - 2
        }.take(3)
    }

    private fun getPostcodeCityRegionMap(): Map<String, Pair<String, String>> {
        return mapOf(
            // Auckland
            "1010" to Pair("Auckland", "Auckland"),
            "1011" to Pair("Auckland", "Auckland"),
            "1021" to Pair("Auckland", "Auckland"),
            "1023" to Pair("Auckland", "Auckland"),
            "1051" to Pair("Auckland", "Auckland"),
            "1071" to Pair("Auckland", "Auckland"),
            "1081" to Pair("Auckland", "Auckland"),
            "2016" to Pair("Auckland", "Auckland"),
            "2025" to Pair("Auckland", "Auckland"),
            "2104" to Pair("Auckland", "Auckland"),
            "2105" to Pair("Auckland", "Auckland"),

            // Wellington
            "6011" to Pair("Wellington", "Wellington"),
            "6012" to Pair("Wellington", "Wellington"),
            "6021" to Pair("Wellington", "Wellington"),
            "6022" to Pair("Wellington", "Wellington"),
            "5010" to Pair("Lower Hutt", "Wellington"),
            "5011" to Pair("Lower Hutt", "Wellington"),
            "5018" to Pair("Upper Hutt", "Wellington"),
            "5019" to Pair("Upper Hutt", "Wellington"),

            // Canterbury
            "8011" to Pair("Christchurch", "Canterbury"),
            "8013" to Pair("Christchurch", "Canterbury"),
            "8014" to Pair("Christchurch", "Canterbury"),
            "8022" to Pair("Christchurch", "Canterbury"),
            "8023" to Pair("Christchurch", "Canterbury"),
            "8041" to Pair("Christchurch", "Canterbury"),
            "8051" to Pair("Christchurch", "Canterbury"),
            "7910" to Pair("Timaru", "Canterbury"),

            // Other major cities
            "9016" to Pair("Dunedin", "Otago"),
            "9018" to Pair("Dunedin", "Otago"),
            "3110" to Pair("Hamilton", "Waikato"),
            "3112" to Pair("Hamilton", "Waikato"),
            "3116" to Pair("Hamilton", "Waikato"),
            "4410" to Pair("Palmerston North", "Manawatu-Wanganui"),
            "7010" to Pair("Nelson", "Nelson"),
            "9810" to Pair("Invercargill", "Southland"),
            "3010" to Pair("Rotorua", "Bay of Plenty"),
            "4110" to Pair("Napier", "Hawke's Bay"),
            "4310" to Pair("New Plymouth", "Taranaki"),
            "0110" to Pair("Whangarei", "Northland"),
            "4010" to Pair("Gisborne", "Gisborne"),
        )
    }

    private fun getSimilarPostcodes(postcode: String): List<String> {
        if (postcode.length != 4 || !postcode.all { it.isDigit() }) {
            return emptyList()
        }

        return getPostcodeCityRegionMap().keys.filter { validPostcode ->
            calculateLevenshteinDistance(postcode, validPostcode) <= 1
        }.take(5)
    }

    private fun findSimilarPostcodes(postcode: String): List<String> {
        val allPostcodes = getPostcodeCityRegionMap().keys
        return allPostcodes.filter { validPostcode ->
            val distance = calculateLevenshteinDistance(postcode, validPostcode)
            distance <= 2
        }.sortedBy { calculateLevenshteinDistance(postcode, it) }.take(5)
    }

    private fun getKnownAddressDatabase(): List<AddressComponents> {
        return listOf(
            AddressComponents(streetNumber = "1", streetName = "Queen Street", city = "Auckland", postcode = "1010"),
            AddressComponents(streetNumber = "2", streetName = "Queen Street", city = "Auckland", postcode = "1010"),
            AddressComponents(streetName = "Queen Street", city = "Auckland", postcode = "1010"),
            AddressComponents(streetNumber = "1", streetName = "Lambton Quay", city = "Wellington", postcode = "6011"),
            AddressComponents(streetName = "Lambton Quay", city = "Wellington", postcode = "6011"),
            AddressComponents(
                streetNumber = "100",
                streetName = "Victoria Street",
                city = "Auckland",
                postcode = "1010",
            ),
            AddressComponents(streetNumber = "50", streetName = "The Terrace", city = "Wellington", postcode = "6011"),
            AddressComponents(
                streetNumber = "123",
                streetName = "Cashel Street",
                city = "Christchurch",
                postcode = "8011",
            ),
            AddressComponents(
                streetNumber = "88",
                streetName = "Shortland Street",
                city = "Auckland",
                postcode = "1010",
            ),
            AddressComponents(
                streetNumber = "25",
                streetName = "Featherston Street",
                city = "Wellington",
                postcode = "6011",
            ),
        )
    }

    private fun findExactMatches(input: String, database: List<AddressComponents>): List<AddressSuggestion> {
        return database.filter { component ->
            val fullAddress = standardizeAddress(component).lowercase()
            fullAddress.contains(input)
        }.map { component ->
            AddressSuggestion(
                address = standardizeAddress(component),
                components = component,
                score = 1.0,
                type = SuggestionType.EXACT_MATCH,
            )
        }
    }

    private fun findPartialMatches(input: String, database: List<AddressComponents>): List<AddressSuggestion> {
        return database.filter { component ->
            val streetName = component.streetName?.lowercase() ?: ""
            val city = component.city.lowercase()
            streetName.contains(input) || city.contains(input)
        }.map { component ->
            AddressSuggestion(
                address = standardizeAddress(component),
                components = component,
                score = 0.8,
                type = SuggestionType.PARTIAL_MATCH,
            )
        }
    }

    private fun findSimilarAddresses(input: String, database: List<AddressComponents>): List<AddressSuggestion> {
        return database.filter { component ->
            val fullAddress = standardizeAddress(component).lowercase()
            val distance = calculateLevenshteinDistance(input, fullAddress)
            distance <= input.length / 2
        }.map { component ->
            AddressSuggestion(
                address = standardizeAddress(component),
                components = component,
                score = 0.6,
                type = SuggestionType.SIMILAR_ADDRESS,
            )
        }
    }

    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1, // deletion
                    dp[i][j - 1] + 1, // insertion
                    dp[i - 1][j - 1] + cost, // substitution
                )
            }
        }

        return dp[len1][len2]
    }
}
