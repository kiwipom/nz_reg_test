package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.entity.DirectorStatus
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.DirectorRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class DirectorService(
    private val directorRepository: DirectorRepository,
    private val auditService: AuditService,
    private val residencyValidationService: ResidencyValidationService,
    private val disqualificationService: DirectorDisqualificationService,
    private val notificationService: DirectorNotificationService,
) {
    private val logger = LoggerFactory.getLogger(DirectorService::class.java)

    fun appointDirector(director: Director): Director {
        logger.info("Appointing director: ${director.fullName} for company: ${director.company.id}")

        // Validate that director doesn't already exist for this company
        if (directorRepository.existsByCompanyIdAndFullNameIgnoreCase(
                director.company.id!!,
                director.fullName,
            )
        ) {
            throw ValidationException("fullName", "Director with this name already exists for the company")
        }

        // Validate residency requirements using enhanced validation
        residencyValidationService.validateDirectorResidency(director)

        // Check for director disqualifications
        disqualificationService.validateAppointmentEligibility(
            director.fullName,
            director.dateOfBirth,
            director.company.id!!,
        )

        // Check if company will meet minimum director requirements
        validateDirectorRequirements(director.company.id!!)

        val savedDirector = directorRepository.save(director)
        auditService.logDirectorAppointment(savedDirector.id, savedDirector.company.id!!, savedDirector.fullName)

        // Send appointment notifications
        notificationService.notifyDirectorAppointment(savedDirector)

        logger.info("Director appointed successfully: ${savedDirector.fullName}")
        return savedDirector
    }

    fun resignDirector(directorId: Long, resignationDate: LocalDate = LocalDate.now()): Director {
        val director = getDirectorById(directorId)

        if (director.status != DirectorStatus.ACTIVE) {
            throw ValidationException("status", "Only active directors can resign")
        }

        // Check if this resignation would violate minimum director requirements
        val remainingActiveDirectors = directorRepository.countActiveDirectorsByCompanyId(director.company.id!!)
        if (remainingActiveDirectors <= 1) {
            throw ValidationException("resignation", "Cannot resign - company must have at least one director")
        }

        // Check residency requirements after resignation using enhanced validation
        residencyValidationService.validateResignationWontViolateResidency(director)

        director.resign(resignationDate)
        val savedDirector = directorRepository.save(director)

        auditService.logDirectorResignation(directorId, director.company.id!!, director.fullName, resignationDate)

        // Send resignation notifications
        notificationService.notifyDirectorResignation(savedDirector, resignationDate)

        logger.info("Director resigned: ${director.fullName}")

        return savedDirector
    }

    fun updateDirector(directorId: Long, updatedDirector: Director): Director {
        val existingDirector = getDirectorById(directorId)

        // Update mutable fields
        existingDirector.fullName = updatedDirector.fullName
        existingDirector.dateOfBirth = updatedDirector.dateOfBirth
        existingDirector.placeOfBirth = updatedDirector.placeOfBirth
        existingDirector.residentialAddressLine1 = updatedDirector.residentialAddressLine1
        existingDirector.residentialAddressLine2 = updatedDirector.residentialAddressLine2
        existingDirector.residentialCity = updatedDirector.residentialCity
        existingDirector.residentialRegion = updatedDirector.residentialRegion
        existingDirector.residentialPostcode = updatedDirector.residentialPostcode
        existingDirector.residentialCountry = updatedDirector.residentialCountry
        existingDirector.isNzResident = updatedDirector.isNzResident
        existingDirector.isAustralianResident = updatedDirector.isAustralianResident

        // Validate residency requirements using enhanced validation
        val residencyUpdate = ResidencyUpdate(
            isNzResident = updatedDirector.isNzResident,
            isAustralianResident = updatedDirector.isAustralianResident,
            residentialCountry = updatedDirector.residentialCountry,
        )
        residencyValidationService.validateResidencyUpdate(existingDirector, residencyUpdate)

        val savedDirector = directorRepository.save(existingDirector)
        auditService.logDirectorUpdate(directorId, existingDirector.company.id!!, existingDirector.fullName)

        // Determine updated fields and send notifications
        val updatedFields = determineUpdatedFields(existingDirector, updatedDirector)
        val isSignificantChange = isSignificantChange(updatedFields)
        notificationService.notifyDirectorUpdate(savedDirector, updatedFields, isSignificantChange)

        logger.info("Director updated: ${existingDirector.fullName}")
        return savedDirector
    }

    fun giveDirectorConsent(directorId: Long, consentDate: LocalDate = LocalDate.now()): Director {
        val director = getDirectorById(directorId)

        if (director.consentGiven) {
            throw ValidationException("consent", "Director has already given consent")
        }

        director.giveConsent(consentDate)
        val savedDirector = directorRepository.save(director)

        auditService.logDirectorConsent(directorId, director.company.id!!, director.fullName, consentDate)

        // Send consent notifications
        notificationService.notifyDirectorConsent(savedDirector, consentDate)

        logger.info("Director consent given: ${director.fullName}")

        return savedDirector
    }

    @Transactional(readOnly = true)
    fun getDirectorById(directorId: Long): Director {
        return directorRepository.findById(directorId)
            .orElseThrow { ResourceNotFoundException("director", "Director not found with id: $directorId") }
    }

    @Transactional(readOnly = true)
    fun getDirectorsByCompany(companyId: Long): List<Director> {
        return directorRepository.findByCompanyId(companyId)
    }

    @Transactional(readOnly = true)
    fun getActiveDirectorsByCompany(companyId: Long): List<Director> {
        return directorRepository.findByCompanyIdAndStatus(companyId, DirectorStatus.ACTIVE)
    }

    @Transactional(readOnly = true)
    fun getDirectorsRequiringConsent(companyId: Long): List<Director> {
        return directorRepository.findDirectorsRequiringConsent(companyId)
    }

    @Transactional(readOnly = true)
    fun searchDirectors(searchTerm: String): List<Director> {
        logger.info("Searching directors with term: $searchTerm")
        val results = directorRepository.searchDirectors(searchTerm)
        auditService.logDirectorSearch(searchTerm, results.size)
        return results
    }

    @Transactional(readOnly = true)
    fun getRecentlyAppointedDirectors(companyId: Long, days: Int = 30): List<Director> {
        val fromDate = LocalDate.now().minusDays(days.toLong())
        return directorRepository.findRecentlyAppointedDirectors(companyId, fromDate)
    }

    @Transactional(readOnly = true)
    fun validateCompanyDirectorCompliance(companyId: Long): Map<String, Boolean> {
        val activeDirectorCount = directorRepository.countActiveDirectorsByCompanyId(companyId)
        val residentDirectorCount = directorRepository.countResidentDirectorsByCompanyId(companyId)
        val directorsRequiringConsent = directorRepository.findDirectorsRequiringConsent(companyId)

        return mapOf(
            "hasMinimumDirectors" to (activeDirectorCount >= 1),
            "hasResidentDirector" to (residentDirectorCount >= 1),
            "allDirectorsHaveConsent" to directorsRequiringConsent.isEmpty(),
        )
    }

    private fun validateDirectorRequirements(companyId: Long) {
        // Additional validation logic can be added here
        // For example, checking against disqualified directors database
        logger.debug("Validating director requirements for company: $companyId")
    }

    private fun determineUpdatedFields(existing: Director, updated: Director): List<String> {
        val updatedFields = mutableListOf<String>()

        if (existing.fullName != updated.fullName) updatedFields.add("Full Name")
        if (existing.dateOfBirth != updated.dateOfBirth) updatedFields.add("Date of Birth")
        if (existing.placeOfBirth != updated.placeOfBirth) updatedFields.add("Place of Birth")
        if (existing.residentialAddressLine1 != updated.residentialAddressLine1) {
            updatedFields.add(
                "Residential Address",
            )
        }
        if (existing.residentialAddressLine2 != updated.residentialAddressLine2) {
            updatedFields.add(
                "Residential Address",
            )
        }
        if (existing.residentialCity != updated.residentialCity) updatedFields.add("Residential City")
        if (existing.residentialRegion != updated.residentialRegion) updatedFields.add("Residential Region")
        if (existing.residentialPostcode != updated.residentialPostcode) updatedFields.add("Residential Postcode")
        if (existing.residentialCountry != updated.residentialCountry) updatedFields.add("Residential Country")
        if (existing.isNzResident != updated.isNzResident) updatedFields.add("NZ Residency Status")
        if (existing.isAustralianResident != updated.isAustralianResident) {
            updatedFields.add(
                "Australian Residency Status",
            )
        }

        return updatedFields.distinct()
    }

    private fun isSignificantChange(updatedFields: List<String>): Boolean {
        val significantFields = setOf(
            "Full Name",
            "NZ Residency Status",
            "Australian Residency Status",
            "Residential Country",
        )

        return updatedFields.any { it in significantFields }
    }

    fun disqualifyDirector(
        directorId: Long,
        reason: String,
        disqualificationType: DisqualificationType = DisqualificationType.OTHER,
    ): Director {
        logger.info("Disqualifying director ID: $directorId using enhanced service")

        val result = disqualificationService.disqualifyDirector(
            directorId = directorId,
            disqualificationType = disqualificationType,
            reason = reason,
        )

        auditService.logDirectorDisqualification(
            directorId,
            result.director.company.id!!,
            result.director.fullName,
            reason,
        )

        // Send disqualification notifications
        notificationService.notifyDirectorDisqualification(result.director, reason)

        return result.director
    }

    fun deleteDirector(directorId: Long) {
        val director = getDirectorById(directorId)

        // Check if this is the only active director
        val activeDirectorCount = directorRepository.countActiveDirectorsByCompanyId(director.company.id!!)
        if (activeDirectorCount <= 1 && director.status == DirectorStatus.ACTIVE) {
            throw ValidationException("deletion", "Cannot delete - company must have at least one director")
        }

        directorRepository.delete(director)
        auditService.logDirectorDeletion(directorId, director.company.id!!, director.fullName)
        logger.info("Director deleted: ${director.fullName}")
    }
}
