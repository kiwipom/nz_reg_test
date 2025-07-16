package nz.govt.companiesoffice.register.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import nz.govt.companiesoffice.register.entity.Company
import nz.govt.companiesoffice.register.entity.Director
import nz.govt.companiesoffice.register.entity.Document
import nz.govt.companiesoffice.register.entity.Shareholder
import nz.govt.companiesoffice.register.service.CompanyService
import nz.govt.companiesoffice.register.service.DirectorService
import nz.govt.companiesoffice.register.service.DocumentService
import nz.govt.companiesoffice.register.service.ShareholderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Advanced search operations across all entities")
class SearchController(
    private val companyService: CompanyService,
    private val directorService: DirectorService,
    private val shareholderService: ShareholderService,
    private val documentService: DocumentService,
) {

    @GetMapping("/global")
    @Operation(summary = "Global search across all entities")
    fun globalSearch(@RequestParam query: String): ResponseEntity<Map<String, Any>> {
        val companies = companyService.searchCompanies(query)
        val directors = directorService.searchDirectors(query)
        val shareholders = shareholderService.searchShareholders(query)
        val documents = emptyList<Document>() // DocumentService doesn't have searchDocuments method

        val results = mapOf(
            "companies" to companies,
            "directors" to directors,
            "shareholders" to shareholders,
            "documents" to documents,
            "totalResults" to (companies.size + directors.size + shareholders.size + documents.size),
        )

        return ResponseEntity.ok(results)
    }

    @GetMapping("/companies")
    @Operation(summary = "Search companies with filters")
    fun searchCompanies(
        @RequestParam query: String,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) incorporationYear: Int?,
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<List<Company>> {
        // For now, use basic search - can be extended with filters
        val companies = companyService.searchCompanies(query)

        var filteredCompanies = companies

        // Apply status filter if provided
        status?.let { statusFilter ->
            filteredCompanies = filteredCompanies.filter {
                it.status.toString().equals(statusFilter, true)
            }
        }

        // Apply incorporation year filter if provided
        incorporationYear?.let { year ->
            filteredCompanies = filteredCompanies.filter {
                it.incorporationDate?.year == year
            }
        }

        // Apply limit if provided
        limit?.let { maxResults ->
            filteredCompanies = filteredCompanies.take(maxResults)
        }

        return ResponseEntity.ok(filteredCompanies)
    }

    @GetMapping("/directors")
    @Operation(summary = "Search directors with filters")
    fun searchDirectors(
        @RequestParam query: String,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) isResident: Boolean?,
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<List<Director>> {
        var directors = directorService.searchDirectors(query)

        // Apply status filter if provided
        status?.let { statusFilter ->
            directors = directors.filter {
                it.status.toString().equals(statusFilter, true)
            }
        }

        // Apply residency filter if provided
        isResident?.let { resident ->
            directors = directors.filter { it.isResident() == resident }
        }

        // Apply limit if provided
        limit?.let { maxResults ->
            directors = directors.take(maxResults)
        }

        return ResponseEntity.ok(directors)
    }

    @GetMapping("/shareholders")
    @Operation(summary = "Search shareholders with filters")
    fun searchShareholders(
        @RequestParam query: String,
        @RequestParam(required = false) country: String?,
        @RequestParam(required = false) isIndividual: Boolean?,
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<List<Shareholder>> {
        var shareholders = shareholderService.searchShareholders(query)

        // Apply country filter if provided
        country?.let { countryFilter ->
            shareholders = shareholders.filter {
                it.country.equals(countryFilter, true)
            }
        }

        // Apply individual/corporate filter if provided
        isIndividual?.let { individual ->
            shareholders = shareholders.filter { it.isIndividual == individual }
        }

        // Apply limit if provided
        limit?.let { maxResults ->
            shareholders = shareholders.take(maxResults)
        }

        return ResponseEntity.ok(shareholders)
    }

    @GetMapping("/documents")
    @Operation(summary = "Search documents with filters")
    fun searchDocuments(
        @RequestParam query: String,
        @RequestParam(required = false) documentType: String?,
        @RequestParam(required = false) mimeType: String?,
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<List<Document>> {
        // For now, return empty list as DocumentService doesn't have searchDocuments method
        val documents = emptyList<Document>()
        return ResponseEntity.ok(documents)
    }

    @GetMapping("/advanced")
    @Operation(summary = "Advanced search with complex filters")
    fun advancedSearch(
        @RequestParam query: String,
        @RequestParam(required = false) entityTypes: List<String>?,
        @RequestParam(required = false) dateFrom: String?,
        @RequestParam(required = false) dateTo: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortOrder: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) pageSize: Int?,
    ): ResponseEntity<Map<String, Any>> {
        val results = mutableMapOf<String, Any>()
        var totalResults = 0

        // Default entity types if none specified
        val searchEntityTypes = entityTypes ?: listOf("companies", "directors", "shareholders", "documents")

        // Search companies if requested
        if (searchEntityTypes.contains("companies")) {
            val companies = companyService.searchCompanies(query)
            results["companies"] = companies
            totalResults += companies.size
        }

        // Search directors if requested
        if (searchEntityTypes.contains("directors")) {
            val directors = directorService.searchDirectors(query)
            results["directors"] = directors
            totalResults += directors.size
        }

        // Search shareholders if requested
        if (searchEntityTypes.contains("shareholders")) {
            val shareholders = shareholderService.searchShareholders(query)
            results["shareholders"] = shareholders
            totalResults += shareholders.size
        }

        // Search documents if requested
        if (searchEntityTypes.contains("documents")) {
            val documents = emptyList<Document>() // DocumentService doesn't have searchDocuments method
            results["documents"] = documents
            totalResults += documents.size
        }

        // Add pagination info
        val currentPage = page ?: 1
        val currentPageSize = pageSize ?: 50
        val totalPages = (totalResults + currentPageSize - 1) / currentPageSize

        results["pagination"] = mapOf(
            "currentPage" to currentPage,
            "pageSize" to currentPageSize,
            "totalResults" to totalResults,
            "totalPages" to totalPages,
        )

        results["searchMetadata"] = mapOf(
            "query" to query,
            "entityTypes" to searchEntityTypes,
            "sortBy" to (sortBy ?: "relevance"),
            "sortOrder" to (sortOrder ?: "desc"),
        )

        return ResponseEntity.ok(results)
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions")
    fun getSearchSuggestions(@RequestParam query: String): ResponseEntity<Map<String, List<String>>> {
        val suggestions = mutableMapOf<String, List<String>>()

        // Get company name suggestions
        val companies = companyService.searchCompanies(query).take(5)
        suggestions["companies"] = companies.map { it.companyName }

        // Get director name suggestions
        val directors = directorService.searchDirectors(query).take(5)
        suggestions["directors"] = directors.map { it.fullName }

        // Get shareholder name suggestions
        val shareholders = shareholderService.searchShareholders(query).take(5)
        suggestions["shareholders"] = shareholders.map { it.fullName }

        return ResponseEntity.ok(suggestions)
    }

    @GetMapping("/stats")
    @Operation(summary = "Get search statistics")
    fun getSearchStatistics(): ResponseEntity<Map<String, Any>> {
        // This would typically come from a search analytics service
        val stats = mapOf(
            "totalEntities" to mapOf(
                "companies" to "This would be calculated",
                "directors" to "This would be calculated",
                "shareholders" to "This would be calculated",
                "documents" to "This would be calculated",
            ),
            "popularSearchTerms" to listOf(
                "Example Company",
                "John Smith",
                "Auckland",
                "Limited",
                "Director",
            ),
            "searchTrends" to mapOf(
                "todaySearches" to 150,
                "weekSearches" to 1200,
                "monthSearches" to 5000,
            ),
        )

        return ResponseEntity.ok(stats)
    }
}
