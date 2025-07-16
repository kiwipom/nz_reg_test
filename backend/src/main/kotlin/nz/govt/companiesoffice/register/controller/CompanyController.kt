package nz.govt.companiesoffice.register.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.service.CompanyService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/companies")
@Tag(name = "Companies", description = "Company management operations")
class CompanyController(
    private val companyService: CompanyService,
) {

    @GetMapping("/{id}")
    @Operation(summary = "Get company by ID")
    fun getCompanyById(@PathVariable id: Long): ResponseEntity<Company> {
        val company = companyService.getCompanyById(id)
        return ResponseEntity.ok(company)
    }

    @GetMapping("/number/{companyNumber}")
    @Operation(summary = "Get company by company number")
    fun getCompanyByNumber(@PathVariable companyNumber: String): ResponseEntity<Company> {
        val company = companyService.getCompanyByNumber(companyNumber)
        return ResponseEntity.ok(company)
    }

    @GetMapping("/search")
    @Operation(summary = "Search companies")
    fun searchCompanies(@RequestParam query: String): ResponseEntity<List<Company>> {
        val companies = companyService.searchCompanies(query)
        return ResponseEntity.ok(companies)
    }

    @GetMapping
    @Operation(summary = "Get all active companies")
    fun getActiveCompanies(): ResponseEntity<List<Company>> {
        val companies = companyService.getActiveCompanies()
        return ResponseEntity.ok(companies)
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Create new company",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun createCompany(@RequestBody company: Company): ResponseEntity<Company> {
        val createdCompany = companyService.createCompany(company)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Update company",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun updateCompany(
        @PathVariable id: Long,
        @RequestBody company: Company,
    ): ResponseEntity<Company> {
        val updatedCompany = companyService.updateCompany(id, company)
        return ResponseEntity.ok(updatedCompany)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete company",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun deleteCompany(@PathVariable id: Long): ResponseEntity<Void> {
        companyService.deleteCompany(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/check-name")
    @Operation(summary = "Check company name availability")
    fun checkNameAvailability(@RequestParam name: String): ResponseEntity<Map<String, Boolean>> {
        val isAvailable = companyService.isCompanyNameAvailable(name)
        return ResponseEntity.ok(mapOf("available" to isAvailable))
    }

    @GetMapping("/check-number")
    @Operation(summary = "Check company number availability")
    fun checkNumberAvailability(@RequestParam number: String): ResponseEntity<Map<String, Boolean>> {
        val isAvailable = companyService.isCompanyNumberAvailable(number)
        return ResponseEntity.ok(mapOf("available" to isAvailable))
    }
}
