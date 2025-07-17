package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.DirectorRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service for validating director residency requirements according to the Companies Act 1993.
 *
 * Key requirements:
 * - Companies must have at least one director who is ordinarily resident in New Zealand or Australia
 * - Directors can be NZ citizens, Australian citizens, or hold valid residence permits
 * - Residency status must be validated and maintained
 */
@Service
class ResidencyValidationService(
    private val directorRepository: DirectorRepository,
    @Value("\${app.compliance.directors.min-nz-au-residents:1}")
    private val minimumResidentDirectors: Int,
) {
    private val logger = LoggerFactory.getLogger(ResidencyValidationService::class.java)

    /**
     * Validates that a director meets the residency requirements.
     */
    fun validateDirectorResidency(director: Director) {
        logger.debug("Validating residency for director: ${director.fullName}")

        if (!director.isNzResident && !director.isAustralianResident) {
            throw ValidationException(
                "residency",
                "Director must be ordinarily resident in New Zealand or Australia as per Companies Act 1993 s151(2)",
            )
        }

        // Additional validation based on residential country
        validateResidentialCountryConsistency(director)
    }

    /**
     * Validates that a company meets the minimum resident director requirements.
     */
    fun validateCompanyResidencyCompliance(companyId: Long) {
        logger.debug("Validating company residency compliance for company: $companyId")

        val residentDirectorCount = directorRepository.countResidentDirectorsByCompanyId(companyId)

        if (residentDirectorCount < minimumResidentDirectors) {
            throw ValidationException(
                "residency_compliance",
                "Company must have at least $minimumResidentDirectors NZ/Australian resident " +
                    "director(s) as per Companies Act 1993 s151(2)",
            )
        }
    }

    /**
     * Validates that removing a director won't violate residency requirements.
     */
    fun validateResignationWontViolateResidency(director: Director) {
        logger.debug("Validating resignation impact for director: ${director.fullName}")

        if (!director.isResident()) {
            // Non-resident director can resign without affecting compliance
            return
        }

        val companyId = director.company.id!!
        val currentResidentCount = directorRepository.countResidentDirectorsByCompanyId(companyId)

        if (currentResidentCount <= minimumResidentDirectors) {
            throw ValidationException(
                "residency_compliance",
                "Cannot resign director - company must maintain at least $minimumResidentDirectors " +
                    "NZ/Australian resident director(s)",
            )
        }
    }

    /**
     * Validates that updating a director's residency status won't violate requirements.
     */
    fun validateResidencyUpdate(
        existingDirector: Director,
        updatedResidency: ResidencyUpdate,
    ) {
        logger.debug("Validating residency update for director: ${existingDirector.fullName}")

        val companyId = existingDirector.company.id!!
        val currentResidentCount = directorRepository.countResidentDirectorsByCompanyId(companyId)

        // Check if this update would remove residency status
        val currentlyResident = existingDirector.isResident()
        val willBeResident = updatedResidency.isNzResident || updatedResidency.isAustralianResident

        if (currentlyResident && !willBeResident) {
            // Director is losing residency status
            if (currentResidentCount <= minimumResidentDirectors) {
                throw ValidationException(
                    "residency_compliance",
                    "Cannot update director residency - company must maintain at least " +
                        "$minimumResidentDirectors NZ/Australian resident director(s)",
                )
            }
        }

        // Validate new residency status
        if (!willBeResident) {
            throw ValidationException(
                "residency",
                "Director must be ordinarily resident in New Zealand or Australia",
            )
        }

        // Validate consistency with residential country
        validateResidentialCountryConsistency(updatedResidency)
    }

    /**
     * Gets detailed residency compliance information for a company.
     */
    fun getCompanyResidencyCompliance(companyId: Long): ResidencyComplianceInfo {
        logger.debug("Getting residency compliance info for company: $companyId")

        val activeDirectors = directorRepository.findByCompanyIdAndStatus(
            companyId,
            nz.govt.companiesoffice.register.entity.DirectorStatus.ACTIVE,
        )
        val residentDirectors = activeDirectors.filter { it.isResident() }
        val nzResidentDirectors = activeDirectors.filter { it.isNzResident }
        val auResidentDirectors = activeDirectors.filter { it.isAustralianResident }

        val isCompliant = residentDirectors.size >= minimumResidentDirectors

        return ResidencyComplianceInfo(
            companyId = companyId,
            totalActiveDirectors = activeDirectors.size,
            residentDirectorCount = residentDirectors.size,
            nzResidentCount = nzResidentDirectors.size,
            australianResidentCount = auResidentDirectors.size,
            minimumRequired = minimumResidentDirectors,
            isCompliant = isCompliant,
            nonCompliantReason = if (!isCompliant) "Insufficient NZ/Australian resident directors" else null,
        )
    }

    /**
     * Validates residency eligibility based on various criteria.
     */
    fun validateResidencyEligibility(
        citizenshipCountry: String?,
        residencePermitCountry: String?,
        residentialCountry: String,
    ): ResidencyEligibilityResult {
        logger.debug("Validating residency eligibility for residential country: $residentialCountry")

        val isNzEligible = when {
            citizenshipCountry == "NZ" -> true
            residencePermitCountry == "NZ" -> true
            residentialCountry == "NZ" -> true
            else -> false
        }

        val isAuEligible = when {
            citizenshipCountry == "AU" -> true
            residencePermitCountry == "AU" -> true
            residentialCountry == "AU" -> true
            else -> false
        }

        return ResidencyEligibilityResult(
            isNzEligible = isNzEligible,
            isAustralianEligible = isAuEligible,
            reasons = buildList {
                if (isNzEligible) {
                    when {
                        citizenshipCountry == "NZ" -> add("New Zealand citizenship")
                        residencePermitCountry == "NZ" -> add("New Zealand residence permit")
                        residentialCountry == "NZ" -> add("Ordinarily resident in New Zealand")
                    }
                }
                if (isAuEligible) {
                    when {
                        citizenshipCountry == "AU" -> add("Australian citizenship")
                        residencePermitCountry == "AU" -> add("Australian residence permit")
                        residentialCountry == "AU" -> add("Ordinarily resident in Australia")
                    }
                }
            },
        )
    }

    private fun validateResidentialCountryConsistency(director: Director) {
        validateResidentialCountryConsistency(
            ResidencyUpdate(
                isNzResident = director.isNzResident,
                isAustralianResident = director.isAustralianResident,
                residentialCountry = director.residentialCountry,
            ),
        )
    }

    private fun validateResidentialCountryConsistency(residencyInfo: ResidencyUpdate) {
        val residentialCountry = residencyInfo.residentialCountry
        val isNzResident = residencyInfo.isNzResident
        val isAustralianResident = residencyInfo.isAustralianResident

        // Warning for potential inconsistencies (not errors, as there can be valid reasons)
        when {
            isNzResident && residentialCountry != "NZ" -> {
                logger.warn("Director marked as NZ resident but residential country is $residentialCountry")
            }
            isAustralianResident && residentialCountry != "AU" -> {
                logger.warn("Director marked as Australian resident but residential country is $residentialCountry")
            }
            !isNzResident && !isAustralianResident && (residentialCountry == "NZ" || residentialCountry == "AU") -> {
                logger.warn("Director has NZ/AU residential country but not marked as resident")
            }
        }
    }
}

/**
 * Data class for residency update information.
 */
data class ResidencyUpdate(
    val isNzResident: Boolean,
    val isAustralianResident: Boolean,
    val residentialCountry: String,
)

/**
 * Data class containing detailed residency compliance information.
 */
data class ResidencyComplianceInfo(
    val companyId: Long,
    val totalActiveDirectors: Int,
    val residentDirectorCount: Int,
    val nzResidentCount: Int,
    val australianResidentCount: Int,
    val minimumRequired: Int,
    val isCompliant: Boolean,
    val nonCompliantReason: String?,
)

/**
 * Data class for residency eligibility results.
 */
data class ResidencyEligibilityResult(
    val isNzEligible: Boolean,
    val isAustralianEligible: Boolean,
    val reasons: List<String>,
)
