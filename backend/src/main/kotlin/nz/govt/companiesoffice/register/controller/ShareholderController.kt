package nz.govt.companiesoffice.register.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import nz.govt.companiesoffice.register.entity.Shareholder
import nz.govt.companiesoffice.register.service.ShareholderService
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
@RequestMapping("/api/v1/shareholders")
@Tag(name = "Shareholders", description = "Shareholder management operations")
class ShareholderController(
    private val shareholderService: ShareholderService,
) {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Create new shareholder",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun createShareholder(@RequestBody shareholder: Shareholder): ResponseEntity<Shareholder> {
        val createdShareholder = shareholderService.createShareholder(shareholder)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdShareholder)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shareholder by ID")
    fun getShareholderById(@PathVariable id: Long): ResponseEntity<Shareholder> {
        val shareholder = shareholderService.getShareholderById(id)
        return ResponseEntity.ok(shareholder)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Update shareholder",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun updateShareholder(
        @PathVariable id: Long,
        @RequestBody shareholder: Shareholder,
    ): ResponseEntity<Shareholder> {
        val updatedShareholder = shareholderService.updateShareholder(id, shareholder)
        return ResponseEntity.ok(updatedShareholder)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete shareholder",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun deleteShareholder(@PathVariable id: Long): ResponseEntity<Void> {
        shareholderService.deleteShareholder(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Get all shareholders for a company")
    fun getShareholdersByCompany(@PathVariable companyId: Long): ResponseEntity<List<Shareholder>> {
        val shareholders = shareholderService.getShareholdersByCompany(companyId)
        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/company/{companyId}/individual")
    @Operation(summary = "Get individual shareholders for a company")
    fun getIndividualShareholdersByCompany(@PathVariable companyId: Long): ResponseEntity<List<Shareholder>> {
        val shareholders = shareholderService.getIndividualShareholdersByCompany(companyId)
        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/company/{companyId}/corporate")
    @Operation(summary = "Get corporate shareholders for a company")
    fun getCorporateShareholdersByCompany(@PathVariable companyId: Long): ResponseEntity<List<Shareholder>> {
        val shareholders = shareholderService.getCorporateShareholdersByCompany(companyId)
        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/company/{companyId}/by-location")
    @Operation(summary = "Get shareholders by location for a company")
    fun getShareholdersByLocation(
        @PathVariable companyId: Long,
        @RequestParam city: String,
        @RequestParam country: String,
    ): ResponseEntity<List<Shareholder>> {
        val shareholders = shareholderService.getShareholdersByLocation(companyId, city, country)
        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/company/{companyId}/by-country")
    @Operation(summary = "Get shareholders by country for a company")
    fun getShareholdersByCountry(
        @PathVariable companyId: Long,
        @RequestParam country: String,
    ): ResponseEntity<List<Shareholder>> {
        val shareholders = shareholderService.getShareholdersByCountry(companyId, country)
        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/company/{companyId}/by-region")
    @Operation(summary = "Get shareholders by region for a company")
    fun getShareholdersByRegion(
        @PathVariable companyId: Long,
        @RequestParam region: String,
    ): ResponseEntity<List<Shareholder>> {
        val shareholders = shareholderService.getShareholdersByRegion(companyId, region)
        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/company/{companyId}/by-postcode")
    @Operation(summary = "Get shareholders by postcode for a company")
    fun getShareholdersByPostcode(
        @PathVariable companyId: Long,
        @RequestParam postcode: String,
    ): ResponseEntity<List<Shareholder>> {
        val shareholders = shareholderService.getShareholdersByPostcode(companyId, postcode)
        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/search")
    @Operation(summary = "Search shareholders")
    fun searchShareholders(@RequestParam query: String): ResponseEntity<List<Shareholder>> {
        val shareholders = shareholderService.searchShareholders(query)
        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/company/{companyId}/search-address")
    @Operation(summary = "Search shareholders by address for a company")
    fun searchShareholdersByAddress(
        @PathVariable companyId: Long,
        @RequestParam address: String,
    ): ResponseEntity<List<Shareholder>> {
        val shareholders = shareholderService.searchShareholdersByAddress(companyId, address)
        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/company/{companyId}/statistics")
    @Operation(summary = "Get shareholder statistics for a company")
    fun getShareholderStatistics(@PathVariable companyId: Long): ResponseEntity<Map<String, Any>> {
        val statistics = shareholderService.getShareholderStatistics(companyId)
        return ResponseEntity.ok(statistics)
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Validate shareholder data",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun validateShareholderData(@PathVariable id: Long): ResponseEntity<Map<String, Boolean>> {
        val shareholder = shareholderService.getShareholderById(id)
        val validation = shareholderService.validateShareholderData(shareholder)
        return ResponseEntity.ok(validation)
    }
}
