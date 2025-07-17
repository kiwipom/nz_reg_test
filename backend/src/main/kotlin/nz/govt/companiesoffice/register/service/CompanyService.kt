package nz.govt.companiesoffice.register.service

import nz.govt.companiesoffice.register.audit.AuditService
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.CompanyType
import nz.govt.companiesoffice.register.exception.ResourceNotFoundException
import nz.govt.companiesoffice.register.exception.ValidationException
import nz.govt.companiesoffice.register.repository.CompanyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompanyService(
    private val companyRepository: CompanyRepository,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(CompanyService::class.java)

    fun createCompany(company: Company): Company {
        logger.info("Creating company: ${company.companyName}")

        // Validate company name uniqueness
        if (companyRepository.existsByCompanyNameIgnoreCase(company.companyName)) {
            throw ValidationException("companyName", "Company name already exists")
        }

        // Validate company number uniqueness
        if (companyRepository.existsByCompanyNumber(company.companyNumber)) {
            throw ValidationException("companyNumber", "Company number already exists")
        }

        // Validate NZBN uniqueness if provided
        company.nzbn?.let { nzbn ->
            if (companyRepository.findByNzbn(nzbn) != null) {
                throw ValidationException("nzbn", "NZBN already exists")
            }
        }

        val savedCompany = companyRepository.save(company)
        auditService.logCompanyCreation(savedCompany.id!!, savedCompany.companyName)
        logger.info("Company created successfully: ${savedCompany.companyNumber}")
        return savedCompany
    }

    @Transactional(readOnly = true)
    fun getCompanyById(id: Long): Company {
        val company = companyRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("company", "Company not found with id: $id") }
        auditService.logCompanyAccess(id)
        return company
    }

    @Transactional(readOnly = true)
    fun getCompanyByNumber(companyNumber: String): Company {
        return companyRepository.findByCompanyNumber(companyNumber)
            ?: throw ResourceNotFoundException("company", "Company not found with number: $companyNumber")
    }

    @Transactional(readOnly = true)
    fun searchCompanies(query: String): List<Company> {
        logger.info("Searching companies with query: $query")
        val results = companyRepository.searchCompanies(query)
        auditService.logCompanySearch(query, results.size)
        return results
    }

    @Transactional(readOnly = true)
    fun getCompaniesByType(companyType: CompanyType): List<Company> {
        return companyRepository.findByCompanyType(companyType)
    }

    @Transactional(readOnly = true)
    fun getActiveCompanies(): List<Company> {
        return companyRepository.findByStatus("ACTIVE")
    }

    fun updateCompany(id: Long, updatedCompany: Company): Company {
        val existingCompany = getCompanyById(id)

        // Validate company name uniqueness if it's being changed
        if (existingCompany.companyName != updatedCompany.companyName) {
            if (companyRepository.existsByCompanyNameIgnoreCase(updatedCompany.companyName)) {
                throw ValidationException("companyName", "Company name already exists")
            }
        }

        // Validate NZBN uniqueness if it's being changed
        if (existingCompany.nzbn != updatedCompany.nzbn) {
            updatedCompany.nzbn?.let { nzbn ->
                if (companyRepository.findByNzbn(nzbn) != null) {
                    throw ValidationException("nzbn", "NZBN already exists")
                }
            }
        }

        // Update mutable fields
        existingCompany.companyName = updatedCompany.companyName
        existingCompany.nzbn = updatedCompany.nzbn
        existingCompany.status = updatedCompany.status

        val savedCompany = companyRepository.save(existingCompany)
        logger.info("Company updated successfully: ${savedCompany.companyNumber}")
        return savedCompany
    }

    fun deleteCompany(id: Long) {
        val company = getCompanyById(id)
        companyRepository.delete(company)
        logger.info("Company deleted: ${company.companyNumber}")
    }

    @Transactional(readOnly = true)
    fun isCompanyNameAvailable(companyName: String): Boolean {
        return !companyRepository.existsByCompanyNameIgnoreCase(companyName)
    }

    @Transactional(readOnly = true)
    fun isCompanyNumberAvailable(companyNumber: String): Boolean {
        return !companyRepository.existsByCompanyNumber(companyNumber)
    }
}
