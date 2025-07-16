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

        // Validate residency requirements
        director.validateResidencyRequirement()

        // Check if company will meet minimum director requirements
        validateDirectorRequirements(director.company.id!!)

        val savedDirector = directorRepository.save(director)
        auditService.logDirectorAppointment(savedDirector.id, savedDirector.company.id!!, savedDirector.fullName)

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

        // Check NZ residency requirements after resignation
        val remainingResidentDirectors = directorRepository.countResidentDirectorsByCompanyId(director.company.id!!)
        if (remainingResidentDirectors <= 1 && director.isResident()) {
            throw ValidationException(
                "resignation",
                "Cannot resign - company must have at least one NZ/Australian resident director",
            )
        }

        director.resign(resignationDate)
        val savedDirector = directorRepository.save(director)

        auditService.logDirectorResignation(directorId, director.company.id!!, director.fullName, resignationDate)
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

        // Validate residency requirements
        existingDirector.validateResidencyRequirement()

        val savedDirector = directorRepository.save(existingDirector)
        auditService.logDirectorUpdate(directorId, existingDirector.company.id!!, existingDirector.fullName)

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

    fun disqualifyDirector(directorId: Long, reason: String): Director {
        val director = getDirectorById(directorId)

        if (director.status == DirectorStatus.DISQUALIFIED) {
            throw ValidationException("status", "Director is already disqualified")
        }

        director.status = DirectorStatus.DISQUALIFIED
        val savedDirector = directorRepository.save(director)

        auditService.logDirectorDisqualification(directorId, director.company.id!!, director.fullName, reason)
        logger.info("Director disqualified: ${director.fullName} - Reason: $reason")

        return savedDirector
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
