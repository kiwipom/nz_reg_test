package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.entity.DirectorStatus
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.DirectorRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Service for managing director disqualifications according to the Companies Act 1993.
 *
 * Handles various disqualification scenarios including:
 * - Court orders (s382-385)
 * - Bankruptcy disqualifications (s383)
 * - Compliance-based disqualifications
 * - Voluntary disqualifications
 * - Age-based restrictions
 */
@Service
@Transactional
class DirectorDisqualificationService(
    private val directorRepository: DirectorRepository,
) {
    private val logger = LoggerFactory.getLogger(DirectorDisqualificationService::class.java)

    /**
     * Checks if a person is disqualified from being a director.
     */
    fun checkDisqualificationStatus(
        fullName: String,
        dateOfBirth: LocalDate? = null,
        otherIdentifiers: Map<String, String> = emptyMap(),
    ): DisqualificationCheckResult {
        logger.debug("Checking disqualification status for: $fullName")

        // Check existing disqualifications in the database
        val existingDisqualifications = findExistingDisqualifications(fullName, dateOfBirth, otherIdentifiers)

        // Check age-based disqualification (must be 18 or older)
        val ageDisqualification = checkAgeDisqualification(dateOfBirth)

        // Combine all disqualification reasons
        val allDisqualifications = existingDisqualifications + listOfNotNull(ageDisqualification)

        return DisqualificationCheckResult(
            isDisqualified = allDisqualifications.isNotEmpty(),
            disqualifications = allDisqualifications,
            eligibleForAppointment = allDisqualifications.isEmpty(),
        )
    }

    /**
     * Disqualifies a director with comprehensive validation and audit trail.
     */
    fun disqualifyDirector(
        directorId: Long,
        disqualificationType: DisqualificationType,
        reason: String,
        effectiveDate: LocalDate = LocalDate.now(),
        courtOrderDetails: String? = null,
        reviewDate: LocalDate? = null,
    ): DisqualificationResult {
        logger.info("Disqualifying director ID: $directorId, Type: $disqualificationType, Reason: $reason")

        val director = directorRepository.findById(directorId)
            .orElseThrow { ValidationException("director", "Director not found with id: $directorId") }

        // Validate disqualification request
        validateDisqualificationRequest(director, disqualificationType, reason)

        // Check if this would violate company director requirements
        validateCompanyComplianceAfterDisqualification(director)

        // Create disqualification record
        val disqualificationRecord = DisqualificationRecord(
            directorId = directorId,
            directorName = director.fullName,
            companyId = director.company.id!!,
            type = disqualificationType,
            reason = reason,
            effectiveDate = effectiveDate,
            courtOrderDetails = courtOrderDetails,
            reviewDate = reviewDate,
            status = DisqualificationStatus.ACTIVE,
        )

        // Update director status
        director.status = DirectorStatus.DISQUALIFIED
        val savedDirector = directorRepository.save(director)

        logger.info("Director disqualified successfully: ${director.fullName}")

        return DisqualificationResult(
            director = savedDirector,
            disqualificationRecord = disqualificationRecord,
            complianceImpact = assessComplianceImpact(director),
        )
    }

    /**
     * Removes or lifts a disqualification (e.g., after court order expires).
     */
    fun liftDisqualification(
        directorId: Long,
        reason: String,
        effectiveDate: LocalDate = LocalDate.now(),
    ): Director {
        logger.info("Lifting disqualification for director ID: $directorId, Reason: $reason")

        val director = directorRepository.findById(directorId)
            .orElseThrow { ValidationException("director", "Director not found with id: $directorId") }

        if (director.status != DirectorStatus.DISQUALIFIED) {
            throw ValidationException("status", "Director is not currently disqualified")
        }

        // Check if there are any remaining active disqualifications
        val remainingDisqualifications = checkForRemainingDisqualifications(director)
        if (remainingDisqualifications.isNotEmpty()) {
            throw ValidationException(
                "disqualification",
                "Cannot lift disqualification - director has other active disqualifications: " +
                    remainingDisqualifications.joinToString(", "),
            )
        }

        // Update director status back to active
        director.status = DirectorStatus.ACTIVE
        val savedDirector = directorRepository.save(director)

        logger.info("Disqualification lifted for director: ${director.fullName}")
        return savedDirector
    }

    /**
     * Gets comprehensive disqualification information for a director.
     */
    fun getDisqualificationInfo(directorId: Long): DirectorDisqualificationInfo {
        val director = directorRepository.findById(directorId)
            .orElseThrow { ValidationException("director", "Director not found with id: $directorId") }

        val isCurrentlyDisqualified = director.status == DirectorStatus.DISQUALIFIED
        val disqualificationHistory = getDisqualificationHistory(directorId)
        val activeDisqualifications = disqualificationHistory.filter { it.status == DisqualificationStatus.ACTIVE }

        return DirectorDisqualificationInfo(
            directorId = directorId,
            directorName = director.fullName,
            currentStatus = director.status,
            isCurrentlyDisqualified = isCurrentlyDisqualified,
            activeDisqualifications = activeDisqualifications,
            disqualificationHistory = disqualificationHistory,
        )
    }

    /**
     * Validates that a director appointment won't violate disqualification rules.
     */
    fun validateAppointmentEligibility(
        fullName: String,
        dateOfBirth: LocalDate? = null,
        companyId: Long,
    ) {
        logger.debug("Validating appointment eligibility for: $fullName to company: $companyId")

        val disqualificationCheck = checkDisqualificationStatus(fullName, dateOfBirth)

        if (disqualificationCheck.isDisqualified) {
            val reasons = disqualificationCheck.disqualifications.joinToString("; ") { "${it.type}: ${it.reason}" }
            throw ValidationException(
                "disqualification",
                "Cannot appoint director - person is disqualified: $reasons",
            )
        }
    }

    private fun findExistingDisqualifications(
        fullName: String,
        dateOfBirth: LocalDate?,
        otherIdentifiers: Map<String, String>,
    ): List<DisqualificationRecord> {
        // In a real implementation, this would check against:
        // 1. Internal disqualification database
        // 2. External disqualification registers
        // 3. Court order databases
        // For now, we'll check our internal records

        return directorRepository.findDisqualifiedDirectorsByName(fullName)
            .filter { it.status == DirectorStatus.DISQUALIFIED }
            .map { director ->
                DisqualificationRecord(
                    directorId = director.id,
                    directorName = director.fullName,
                    companyId = director.company.id!!,
                    type = DisqualificationType.DATABASE_RECORD,
                    reason = "Previously disqualified director found in system",
                    effectiveDate = LocalDate.now(), // Would be actual disqualification date
                    status = DisqualificationStatus.ACTIVE,
                )
            }
    }

    private fun checkAgeDisqualification(dateOfBirth: LocalDate?): DisqualificationRecord? {
        if (dateOfBirth == null) return null

        val age = LocalDate.now().year - dateOfBirth.year
        val hasHadBirthday = LocalDate.now().dayOfYear >= dateOfBirth.dayOfYear

        val actualAge = if (hasHadBirthday) age else age - 1

        return if (actualAge < 18) {
            DisqualificationRecord(
                directorId = 0L, // Not yet assigned
                directorName = "Unknown",
                companyId = 0L,
                type = DisqualificationType.AGE_RESTRICTION,
                reason = "Person is under 18 years old (current age: $actualAge)",
                effectiveDate = LocalDate.now(),
                status = DisqualificationStatus.ACTIVE,
            )
        } else {
            null
        }
    }

    private fun validateDisqualificationRequest(
        director: Director,
        type: DisqualificationType,
        reason: String,
    ) {
        if (director.status == DirectorStatus.DISQUALIFIED) {
            throw ValidationException("status", "Director is already disqualified")
        }

        if (reason.isBlank()) {
            throw ValidationException("reason", "Disqualification reason is required")
        }

        // Additional validation based on disqualification type
        when (type) {
            DisqualificationType.COURT_ORDER -> {
                // Could validate court order number format, etc.
            }
            DisqualificationType.BANKRUPTCY -> {
                // Could validate bankruptcy details
            }
            DisqualificationType.COMPLIANCE_BREACH -> {
                // Could validate compliance breach details
            }
            else -> {
                // General validation passed
            }
        }
    }

    private fun validateCompanyComplianceAfterDisqualification(director: Director) {
        val companyId = director.company.id!!
        val activeDirectorCount = directorRepository.countActiveDirectorsByCompanyId(companyId)

        if (activeDirectorCount <= 1) {
            throw ValidationException(
                "disqualification",
                "Cannot disqualify director - company must have at least one active director",
            )
        }

        val residentDirectorCount = directorRepository.countResidentDirectorsByCompanyId(companyId)
        if (residentDirectorCount <= 1 && director.isResident()) {
            throw ValidationException(
                "disqualification",
                "Cannot disqualify director - company must have at least one NZ/Australian resident director",
            )
        }
    }

    private fun assessComplianceImpact(director: Director): ComplianceImpact {
        val companyId = director.company.id!!
        val remainingActiveDirectors = directorRepository.countActiveDirectorsByCompanyId(companyId) - 1
        val remainingResidentDirectors = if (director.isResident()) {
            directorRepository.countResidentDirectorsByCompanyId(companyId) - 1
        } else {
            directorRepository.countResidentDirectorsByCompanyId(companyId)
        }

        return ComplianceImpact(
            remainingActiveDirectors = remainingActiveDirectors,
            remainingResidentDirectors = remainingResidentDirectors,
            meetsMinimumDirectors = remainingActiveDirectors >= 1,
            meetsResidencyRequirements = remainingResidentDirectors >= 1,
        )
    }

    private fun checkForRemainingDisqualifications(director: Director): List<String> {
        // In a real implementation, this would check all active disqualification sources
        // For now, we'll return empty as we only track status in the director entity
        return emptyList()
    }

    private fun getDisqualificationHistory(directorId: Long): List<DisqualificationRecord> {
        // In a real implementation, this would fetch from a disqualification_records table
        // For now, we'll return empty history
        return emptyList()
    }
}

/**
 * Types of director disqualifications.
 */
enum class DisqualificationType {
    COURT_ORDER, // Court-ordered disqualification
    BANKRUPTCY, // Bankruptcy-related disqualification
    COMPLIANCE_BREACH, // Compliance violations
    AGE_RESTRICTION, // Under 18 years old
    VOLUNTARY, // Voluntary disqualification
    DATABASE_RECORD, // Found in disqualification database
    OTHER, // Other statutory disqualifications
}

/**
 * Status of a disqualification.
 */
enum class DisqualificationStatus {
    ACTIVE, // Currently effective
    LIFTED, // Disqualification has been lifted
    EXPIRED, // Disqualification has expired
    PENDING, // Pending review/activation
}

/**
 * Comprehensive disqualification record.
 */
data class DisqualificationRecord(
    val directorId: Long,
    val directorName: String,
    val companyId: Long,
    val type: DisqualificationType,
    val reason: String,
    val effectiveDate: LocalDate,
    val courtOrderDetails: String? = null,
    val reviewDate: LocalDate? = null,
    val status: DisqualificationStatus,
)

/**
 * Result of disqualification check.
 */
data class DisqualificationCheckResult(
    val isDisqualified: Boolean,
    val disqualifications: List<DisqualificationRecord>,
    val eligibleForAppointment: Boolean,
)

/**
 * Result of director disqualification action.
 */
data class DisqualificationResult(
    val director: Director,
    val disqualificationRecord: DisqualificationRecord,
    val complianceImpact: ComplianceImpact,
)

/**
 * Impact assessment after disqualification.
 */
data class ComplianceImpact(
    val remainingActiveDirectors: Long,
    val remainingResidentDirectors: Long,
    val meetsMinimumDirectors: Boolean,
    val meetsResidencyRequirements: Boolean,
)

/**
 * Comprehensive director disqualification information.
 */
data class DirectorDisqualificationInfo(
    val directorId: Long,
    val directorName: String,
    val currentStatus: DirectorStatus,
    val isCurrentlyDisqualified: Boolean,
    val activeDisqualifications: List<DisqualificationRecord>,
    val disqualificationHistory: List<DisqualificationRecord>,
)
