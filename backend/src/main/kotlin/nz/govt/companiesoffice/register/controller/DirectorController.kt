package nz.govt.companiesoffice.register.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.service.DirectorDisqualificationInfo
import nz.govt.companiesoffice.register.service.DirectorDisqualificationService
import nz.govt.companiesoffice.register.service.DirectorService
import nz.govt.companiesoffice.register.service.DisqualificationCheckResult
import nz.govt.companiesoffice.register.service.ResidencyComplianceInfo
import nz.govt.companiesoffice.register.service.ResidencyValidationService
import org.springframework.format.annotation.DateTimeFormat
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
import java.time.LocalDate

@RestController
@RequestMapping("/v1/directors")
@Tag(name = "Directors", description = "Director management operations")
class DirectorController(
    private val directorService: DirectorService,
    private val residencyValidationService: ResidencyValidationService,
    private val disqualificationService: DirectorDisqualificationService,
) {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Appoint new director",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun appointDirector(@RequestBody director: Director): ResponseEntity<Director> {
        val appointedDirector = directorService.appointDirector(director)
        return ResponseEntity.status(HttpStatus.CREATED).body(appointedDirector)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get director by ID")
    fun getDirectorById(@PathVariable id: Long): ResponseEntity<Director> {
        val director = directorService.getDirectorById(id)
        return ResponseEntity.ok(director)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Update director",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun updateDirector(
        @PathVariable id: Long,
        @RequestBody director: Director,
    ): ResponseEntity<Director> {
        val updatedDirector = directorService.updateDirector(id, director)
        return ResponseEntity.ok(updatedDirector)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete director",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun deleteDirector(@PathVariable id: Long): ResponseEntity<Void> {
        directorService.deleteDirector(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/resign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Resign director",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun resignDirector(
        @PathVariable id: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        resignationDate: LocalDate?,
    ): ResponseEntity<Director> {
        val director = directorService.resignDirector(id, resignationDate ?: LocalDate.now())
        return ResponseEntity.ok(director)
    }

    @PostMapping("/{id}/consent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Give director consent",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun giveDirectorConsent(
        @PathVariable id: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        consentDate: LocalDate?,
    ): ResponseEntity<Director> {
        val director = directorService.giveDirectorConsent(id, consentDate ?: LocalDate.now())
        return ResponseEntity.ok(director)
    }

    @PostMapping("/{id}/disqualify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Disqualify director",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun disqualifyDirector(
        @PathVariable id: Long,
        @RequestParam reason: String,
    ): ResponseEntity<Director> {
        val director = directorService.disqualifyDirector(id, reason)
        return ResponseEntity.ok(director)
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Get all directors for a company")
    fun getDirectorsByCompany(@PathVariable companyId: Long): ResponseEntity<List<Director>> {
        val directors = directorService.getDirectorsByCompany(companyId)
        return ResponseEntity.ok(directors)
    }

    @GetMapping("/company/{companyId}/active")
    @Operation(summary = "Get active directors for a company")
    fun getActiveDirectorsByCompany(@PathVariable companyId: Long): ResponseEntity<List<Director>> {
        val directors = directorService.getActiveDirectorsByCompany(companyId)
        return ResponseEntity.ok(directors)
    }

    @GetMapping("/company/{companyId}/requiring-consent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Get directors requiring consent for a company",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun getDirectorsRequiringConsent(@PathVariable companyId: Long): ResponseEntity<List<Director>> {
        val directors = directorService.getDirectorsRequiringConsent(companyId)
        return ResponseEntity.ok(directors)
    }

    @GetMapping("/company/{companyId}/recently-appointed")
    @Operation(summary = "Get recently appointed directors for a company")
    fun getRecentlyAppointedDirectors(
        @PathVariable companyId: Long,
        @RequestParam(defaultValue = "30") days: Int,
    ): ResponseEntity<List<Director>> {
        val directors = directorService.getRecentlyAppointedDirectors(companyId, days)
        return ResponseEntity.ok(directors)
    }

    @GetMapping("/search")
    @Operation(summary = "Search directors")
    fun searchDirectors(@RequestParam query: String): ResponseEntity<List<Director>> {
        val directors = directorService.searchDirectors(query)
        return ResponseEntity.ok(directors)
    }

    @GetMapping("/company/{companyId}/compliance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Check director compliance for a company",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun validateCompanyDirectorCompliance(@PathVariable companyId: Long): ResponseEntity<Map<String, Boolean>> {
        val compliance = directorService.validateCompanyDirectorCompliance(companyId)
        return ResponseEntity.ok(compliance)
    }

    @PostMapping("/check-disqualification")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Check if a person is disqualified from being a director",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun checkDisqualificationStatus(
        @RequestParam fullName: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dateOfBirth: LocalDate?,
    ): ResponseEntity<DisqualificationCheckResult> {
        val result = disqualificationService.checkDisqualificationStatus(fullName, dateOfBirth)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}/disqualification-info")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Get comprehensive disqualification information for a director",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun getDisqualificationInfo(@PathVariable id: Long): ResponseEntity<DirectorDisqualificationInfo> {
        val info = disqualificationService.getDisqualificationInfo(id)
        return ResponseEntity.ok(info)
    }

    @PostMapping("/{id}/lift-disqualification")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Lift/remove director disqualification",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun liftDisqualification(
        @PathVariable id: Long,
        @RequestParam reason: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        effectiveDate: LocalDate?,
    ): ResponseEntity<Director> {
        val director = disqualificationService.liftDisqualification(id, reason, effectiveDate ?: LocalDate.now())
        return ResponseEntity.ok(director)
    }

    @GetMapping("/company/{companyId}/residency-compliance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Get detailed residency compliance information for a company",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun getResidencyCompliance(@PathVariable companyId: Long): ResponseEntity<ResidencyComplianceInfo> {
        val compliance = residencyValidationService.getCompanyResidencyCompliance(companyId)
        return ResponseEntity.ok(compliance)
    }
}
