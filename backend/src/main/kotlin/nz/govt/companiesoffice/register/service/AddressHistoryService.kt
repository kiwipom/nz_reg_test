package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.entity.Address
import nz.govt.companiesoffice.register.entity.AddressType
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.repository.AddressRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service for comprehensive address history tracking and reporting
 */
@Service
@Transactional(readOnly = true)
class AddressHistoryService(
    private val addressRepository: AddressRepository,
) {

    private val logger = LoggerFactory.getLogger(AddressHistoryService::class.java)

    /**
     * Gets the complete address history for a company
     */
    fun getCompanyAddressHistory(company: Company): AddressHistory {
        logger.debug("Retrieving complete address history for company ${company.id}")

        val allAddresses = addressRepository.findAddressHistoryByCompany(company)
        val groupedByType = allAddresses.groupBy { it.addressType }

        val typeHistories = AddressType.values().map { type ->
            val addresses = groupedByType[type] ?: emptyList()
            AddressTypeHistory(
                addressType = type,
                addresses = addresses.sortedByDescending { it.effectiveFrom },
                currentAddress = addresses.find { it.isEffective() },
                totalChanges = addresses.size,
                firstRegistered = addresses.minByOrNull { it.effectiveFrom }?.effectiveFrom,
                lastChanged = addresses.maxByOrNull { it.effectiveFrom }?.effectiveFrom,
            )
        }

        return AddressHistory(
            company = company,
            typeHistories = typeHistories,
            totalAddresses = allAddresses.size,
            totalActiveAddresses = typeHistories.count { it.currentAddress != null },
            oldestAddress = allAddresses.minByOrNull { it.effectiveFrom },
            newestAddress = allAddresses.maxByOrNull { it.effectiveFrom },
        )
    }

    /**
     * Gets the address history for a specific address type
     */
    fun getAddressTypeHistory(
        company: Company,
        addressType: AddressType,
        includeInactive: Boolean = true,
    ): AddressTypeHistory {
        logger.debug("Retrieving ${addressType.name} address history for company ${company.id}")

        val addresses = if (includeInactive) {
            addressRepository.findAddressHistoryByCompanyAndType(company, addressType)
        } else {
            listOfNotNull(addressRepository.findEffectiveAddressByCompanyAndType(company, addressType))
        }

        return AddressTypeHistory(
            addressType = addressType,
            addresses = addresses.sortedByDescending { it.effectiveFrom },
            currentAddress = addresses.find { it.isEffective() },
            totalChanges = addresses.size,
            firstRegistered = addresses.minByOrNull { it.effectiveFrom }?.effectiveFrom,
            lastChanged = addresses.maxByOrNull { it.effectiveFrom }?.effectiveFrom,
        )
    }

    /**
     * Gets address changes within a date range
     */
    fun getAddressChangesInPeriod(
        company: Company,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<AddressChange> {
        logger.debug("Retrieving address changes for company ${company.id} between $startDate and $endDate")

        val allAddresses = addressRepository.findAddressHistoryByCompany(company)
        val changes = mutableListOf<AddressChange>()

        // Group by type and analyze changes
        allAddresses.groupBy { it.addressType }.forEach { (type, addresses) ->
            val sortedAddresses = addresses.sortedBy { it.effectiveFrom }

            for (i in 0 until sortedAddresses.size) {
                val current = sortedAddresses[i]
                val previous = if (i > 0) sortedAddresses[i - 1] else null

                // Check if this change falls within the date range
                if (current.effectiveFrom >= startDate && current.effectiveFrom <= endDate) {
                    changes.add(
                        AddressChange(
                            addressType = type,
                            changeDate = current.effectiveFrom,
                            changeType = when {
                                previous == null -> AddressChangeType.INITIAL_REGISTRATION
                                current.isSameAddress(previous) -> AddressChangeType.ADMINISTRATIVE_UPDATE
                                else -> AddressChangeType.ADDRESS_CHANGE
                            },
                            previousAddress = previous,
                            newAddress = current,
                            duration = previous?.let { prev ->
                                java.time.Period.between(prev.effectiveFrom, current.effectiveFrom)
                            },
                        ),
                    )
                }
            }
        }

        return changes.sortedByDescending { it.changeDate }
    }

    /**
     * Gets address at a specific point in time (historical snapshot)
     */
    fun getAddressSnapshot(
        company: Company,
        date: LocalDate,
    ): AddressSnapshot {
        logger.debug("Retrieving address snapshot for company ${company.id} at $date")

        val addresses = AddressType.values().mapNotNull { type ->
            addressRepository.findEffectiveAddressByCompanyAndType(company, type, date)?.let { address ->
                type to address
            }
        }.toMap()

        return AddressSnapshot(
            company = company,
            snapshotDate = date,
            addresses = addresses,
            isComplete = addresses.containsKey(AddressType.REGISTERED),
        )
    }

    /**
     * Analyzes address patterns and provides insights
     */
    fun analyzeAddressPatterns(company: Company): AddressAnalysis {
        logger.debug("Analyzing address patterns for company ${company.id}")

        val history = getCompanyAddressHistory(company)
        val allAddresses = history.typeHistories.flatMap { it.addresses }

        // Analyze geographical distribution
        val cityDistribution = allAddresses.groupBy { it.city }.mapValues { it.value.size }
        val regionDistribution = allAddresses.mapNotNull { it.region }.groupBy { it }.mapValues { it.value.size }
        val countryDistribution = allAddresses.groupBy { it.country }.mapValues { it.value.size }

        // Analyze change frequency
        val changeFrequency = history.typeHistories.associate {
            it.addressType to it.totalChanges
        }

        // Calculate stability metrics
        val averageAddressDuration = history.typeHistories.mapNotNull { typeHistory ->
            if (typeHistory.addresses.size > 1) {
                val changes = typeHistory.addresses.sortedBy { it.effectiveFrom }
                val durations = changes.zipWithNext { current, next ->
                    java.time.Period.between(current.effectiveFrom, next.effectiveFrom).days.toLong()
                }
                durations.average()
            } else {
                null
            }
        }.average()

        return AddressAnalysis(
            company = company,
            totalAddressChanges = allAddresses.size,
            cityDistribution = cityDistribution,
            regionDistribution = regionDistribution,
            countryDistribution = countryDistribution,
            changeFrequencyByType = changeFrequency,
            averageAddressDuration = averageAddressDuration.takeIf { !it.isNaN() },
            mostFrequentCity = cityDistribution.maxByOrNull { it.value }?.key,
            mostFrequentRegion = regionDistribution.maxByOrNull { it.value }?.key,
            hasInternationalAddresses = countryDistribution.keys.any { it != "NZ" },
            stabilityScore = calculateStabilityScore(history),
        )
    }

    /**
     * Finds companies with similar address patterns
     */
    fun findSimilarAddressPatterns(
        company: Company,
        threshold: Double = 0.7,
    ): List<AddressSimilarity> {
        logger.debug("Finding companies with similar address patterns to company ${company.id}")

        val targetAnalysis = analyzeAddressPatterns(company)
        val similarities = mutableListOf<AddressSimilarity>()

        // This would typically query other companies, but for now we'll return empty
        // In a real implementation, this would compare address patterns across companies

        return similarities
    }

    /**
     * Validates address history for compliance and consistency
     */
    fun validateAddressHistory(company: Company): AddressHistoryValidation {
        logger.debug("Validating address history for company ${company.id}")

        val history = getCompanyAddressHistory(company)
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Check for required addresses
        val hasRegisteredAddress = history.typeHistories.any {
            it.addressType == AddressType.REGISTERED && it.currentAddress != null
        }
        if (!hasRegisteredAddress) {
            errors.add("Company must have an active registered address")
        }

        // Check for gaps in address history
        history.typeHistories.forEach { typeHistory ->
            val addresses = typeHistory.addresses.sortedBy { it.effectiveFrom }
            for (i in 0 until addresses.size - 1) {
                val current = addresses[i]
                val next = addresses[i + 1]

                if (current.effectiveTo != null &&
                    current.effectiveTo!!.plusDays(1) != next.effectiveFrom
                ) {
                    warnings.add(
                        "Gap in ${typeHistory.addressType.name} address history " +
                            "between ${current.effectiveTo} and ${next.effectiveFrom}",
                    )
                }
            }
        }

        // Check for overlapping addresses
        history.typeHistories.forEach { typeHistory ->
            val addresses = typeHistory.addresses.sortedBy { it.effectiveFrom }
            for (i in 0 until addresses.size - 1) {
                val current = addresses[i]
                val next = addresses[i + 1]

                if (current.effectiveTo != null &&
                    current.effectiveTo!! >= next.effectiveFrom
                ) {
                    errors.add(
                        "Overlapping ${typeHistory.addressType.name} addresses: " +
                            "${current.effectiveFrom} to ${current.effectiveTo} overlaps with ${next.effectiveFrom}",
                    )
                }
            }
        }

        return AddressHistoryValidation(
            company = company,
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            checkedAt = LocalDateTime.now(),
        )
    }

    private fun calculateStabilityScore(history: AddressHistory): Double {
        // Calculate a stability score based on:
        // 1. Number of address changes (fewer changes = more stable)
        // 2. Time since last change (longer = more stable)
        // 3. Consistency of geographical location (same city/region = more stable)

        val totalChanges = history.typeHistories.sumOf { it.totalChanges }
        val daysSinceLastChange = history.newestAddress?.effectiveFrom?.let { lastChange ->
            java.time.Period.between(lastChange, LocalDate.now()).days.toDouble()
        } ?: 0.0

        // Simple scoring algorithm (0-100 scale)
        val changeScore = maxOf(0.0, 100.0 - (totalChanges * 10.0))
        val timeScore = minOf(100.0, daysSinceLastChange / 365.0 * 50.0) // 50 points for 1+ years

        return (changeScore + timeScore) / 2.0
    }
}

// Data classes for address history tracking

data class AddressHistory(
    val company: Company,
    val typeHistories: List<AddressTypeHistory>,
    val totalAddresses: Int,
    val totalActiveAddresses: Int,
    val oldestAddress: Address?,
    val newestAddress: Address?,
) {
    fun getCurrentAddresses(): Map<AddressType, Address> {
        return typeHistories.mapNotNull { history ->
            history.currentAddress?.let { address ->
                history.addressType to address
            }
        }.toMap()
    }

    fun hasCompleteHistory(): Boolean {
        return typeHistories.any { it.addressType == AddressType.REGISTERED && it.addresses.isNotEmpty() }
    }
}

data class AddressTypeHistory(
    val addressType: AddressType,
    val addresses: List<Address>,
    val currentAddress: Address?,
    val totalChanges: Int,
    val firstRegistered: LocalDate?,
    val lastChanged: LocalDate?,
) {
    fun isActive(): Boolean = currentAddress != null
    fun hasHistory(): Boolean = addresses.isNotEmpty()
    fun getDurationDays(): Long? {
        return if (firstRegistered != null && lastChanged != null) {
            java.time.Period.between(firstRegistered, lastChanged).days.toLong()
        } else {
            null
        }
    }
}

data class AddressChange(
    val addressType: AddressType,
    val changeDate: LocalDate,
    val changeType: AddressChangeType,
    val previousAddress: Address?,
    val newAddress: Address,
    val duration: java.time.Period?,
) {
    fun isSignificantChange(): Boolean {
        return changeType == AddressChangeType.ADDRESS_CHANGE &&
            previousAddress != null &&
            (
                previousAddress.city != newAddress.city ||
                    previousAddress.country != newAddress.country
                )
    }

    fun getChangeDescription(): String {
        return when (changeType) {
            AddressChangeType.INITIAL_REGISTRATION -> "Initial address registration"
            AddressChangeType.ADDRESS_CHANGE ->
                "Address changed from ${previousAddress?.getFullAddress()} to ${newAddress.getFullAddress()}"
            AddressChangeType.ADMINISTRATIVE_UPDATE -> "Administrative update"
        }
    }
}

enum class AddressChangeType {
    INITIAL_REGISTRATION,
    ADDRESS_CHANGE,
    ADMINISTRATIVE_UPDATE,
}

data class AddressSnapshot(
    val company: Company,
    val snapshotDate: LocalDate,
    val addresses: Map<AddressType, Address>,
    val isComplete: Boolean,
) {
    fun getAddress(type: AddressType): Address? = addresses[type]
    fun hasAddress(type: AddressType): Boolean = addresses.containsKey(type)
    fun getRegisteredAddress(): Address? = addresses[AddressType.REGISTERED]
    fun getServiceAddress(): Address? = addresses[AddressType.SERVICE]
    fun getCommunicationAddress(): Address? = addresses[AddressType.COMMUNICATION]
}

data class AddressAnalysis(
    val company: Company,
    val totalAddressChanges: Int,
    val cityDistribution: Map<String, Int>,
    val regionDistribution: Map<String, Int>,
    val countryDistribution: Map<String, Int>,
    val changeFrequencyByType: Map<AddressType, Int>,
    val averageAddressDuration: Double?,
    val mostFrequentCity: String?,
    val mostFrequentRegion: String?,
    val hasInternationalAddresses: Boolean,
    val stabilityScore: Double,
) {
    fun isHighlyStable(): Boolean = stabilityScore > 80.0
    fun isModeratelyStable(): Boolean = stabilityScore in 50.0..80.0
    fun isUnstable(): Boolean = stabilityScore < 50.0

    fun getPrimaryLocation(): String {
        return listOfNotNull(mostFrequentCity, mostFrequentRegion).joinToString(", ")
    }
}

data class AddressSimilarity(
    val company: Company,
    val similarityScore: Double,
    val commonCities: List<String>,
    val commonRegions: List<String>,
    val sharedPatterns: List<String>,
)

data class AddressHistoryValidation(
    val company: Company,
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val checkedAt: LocalDateTime,
) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    fun getIssueCount(): Int = errors.size + warnings.size
}
