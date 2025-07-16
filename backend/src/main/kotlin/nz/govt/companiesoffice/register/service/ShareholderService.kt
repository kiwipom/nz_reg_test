package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Shareholder
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.ShareholderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ShareholderService(
    private val shareholderRepository: ShareholderRepository,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(ShareholderService::class.java)

    fun createShareholder(shareholder: Shareholder): Shareholder {
        logger.info("Creating shareholder: ${shareholder.fullName} for company: ${shareholder.company.id}")

        // Validate that shareholder doesn't already exist for this company
        if (shareholderRepository.existsByCompanyIdAndFullNameIgnoreCase(
                shareholder.company.id!!,
                shareholder.fullName,
            )
        ) {
            throw ValidationException("fullName", "Shareholder with this name already exists for the company")
        }

        val savedShareholder = shareholderRepository.save(shareholder)
        auditService.logShareholderCreation(
            savedShareholder.id,
            savedShareholder.company.id!!,
            savedShareholder.fullName,
        )

        logger.info("Shareholder created successfully: ${savedShareholder.fullName}")
        return savedShareholder
    }

    fun updateShareholder(shareholderId: Long, updatedShareholder: Shareholder): Shareholder {
        val existingShareholder = getShareholderById(shareholderId)

        // Update mutable fields
        existingShareholder.fullName = updatedShareholder.fullName
        existingShareholder.addressLine1 = updatedShareholder.addressLine1
        existingShareholder.addressLine2 = updatedShareholder.addressLine2
        existingShareholder.city = updatedShareholder.city
        existingShareholder.region = updatedShareholder.region
        existingShareholder.postcode = updatedShareholder.postcode
        existingShareholder.country = updatedShareholder.country
        existingShareholder.isIndividual = updatedShareholder.isIndividual

        val savedShareholder = shareholderRepository.save(existingShareholder)
        auditService.logShareholderUpdate(
            shareholderId,
            existingShareholder.company.id!!,
            existingShareholder.fullName,
        )

        logger.info("Shareholder updated: ${existingShareholder.fullName}")
        return savedShareholder
    }

    fun deleteShareholder(shareholderId: Long) {
        val shareholder = getShareholderById(shareholderId)

        shareholderRepository.delete(shareholder)
        auditService.logShareholderDeletion(shareholderId, shareholder.company.id!!, shareholder.fullName)
        logger.info("Shareholder deleted: ${shareholder.fullName}")
    }

    @Transactional(readOnly = true)
    fun getShareholderById(shareholderId: Long): Shareholder {
        return shareholderRepository.findById(shareholderId)
            .orElseThrow { ResourceNotFoundException("shareholder", "Shareholder not found with id: $shareholderId") }
    }

    @Transactional(readOnly = true)
    fun getShareholdersByCompany(companyId: Long): List<Shareholder> {
        return shareholderRepository.findByCompanyId(companyId)
    }

    @Transactional(readOnly = true)
    fun getIndividualShareholdersByCompany(companyId: Long): List<Shareholder> {
        return shareholderRepository.findByCompanyIdAndIsIndividualTrue(companyId)
    }

    @Transactional(readOnly = true)
    fun getCorporateShareholdersByCompany(companyId: Long): List<Shareholder> {
        return shareholderRepository.findByCompanyIdAndIsIndividualFalse(companyId)
    }

    @Transactional(readOnly = true)
    fun getShareholdersByLocation(companyId: Long, city: String, country: String): List<Shareholder> {
        return shareholderRepository.findByCompanyIdAndCityIgnoreCaseAndCountryIgnoreCase(
            companyId,
            city,
            country,
        )
    }

    @Transactional(readOnly = true)
    fun getShareholdersByCountry(companyId: Long, country: String): List<Shareholder> {
        return shareholderRepository.findByCompanyIdAndCountryIgnoreCase(companyId, country)
    }

    @Transactional(readOnly = true)
    fun getShareholdersByRegion(companyId: Long, region: String): List<Shareholder> {
        return shareholderRepository.findByCompanyIdAndRegionIgnoreCase(companyId, region)
    }

    @Transactional(readOnly = true)
    fun getShareholdersByPostcode(companyId: Long, postcode: String): List<Shareholder> {
        return shareholderRepository.findByCompanyIdAndPostcode(companyId, postcode)
    }

    @Transactional(readOnly = true)
    fun searchShareholders(searchTerm: String): List<Shareholder> {
        logger.info("Searching shareholders with term: $searchTerm")
        val results = shareholderRepository.searchShareholders(searchTerm)
        auditService.logShareholderSearch(searchTerm, results.size)
        return results
    }

    @Transactional(readOnly = true)
    fun searchShareholdersByAddress(companyId: Long, addressTerm: String): List<Shareholder> {
        return shareholderRepository.findByAddressContaining(companyId, addressTerm)
    }

    @Transactional(readOnly = true)
    fun getShareholderStatistics(companyId: Long): Map<String, Any> {
        val totalShareholders = shareholderRepository.countShareholdersByCompanyId(companyId)
        val individualShareholders = shareholderRepository.countShareholdersByType(companyId, true)
        val corporateShareholders = shareholderRepository.countShareholdersByType(companyId, false)

        return mapOf(
            "totalShareholders" to totalShareholders,
            "individualShareholders" to individualShareholders,
            "corporateShareholders" to corporateShareholders,
        )
    }

    @Transactional(readOnly = true)
    fun validateShareholderData(shareholder: Shareholder): Map<String, Boolean> {
        return mapOf(
            "hasValidName" to shareholder.fullName.isNotBlank(),
            "hasValidAddress" to (shareholder.addressLine1.isNotBlank() && shareholder.city.isNotBlank()),
            "hasValidCountry" to shareholder.country.isNotBlank(),
        )
    }
}
