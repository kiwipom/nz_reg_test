package nz.govt.companiesoffice.register.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/docs")
@Tag(name = "API Documentation", description = "API documentation and metadata")
class ApiDocumentationController {

    @Value("\${spring.application.name:NZ Companies Register API}")
    private lateinit var applicationName: String

    @GetMapping("/info")
    @Operation(summary = "Get API information")
    fun getApiInfo(): ResponseEntity<Map<String, Any>> {
        val info = mapOf(
            "name" to applicationName,
            "version" to "1.0.0",
            "description" to "API for managing company registrations and compliance under the Companies Act 1993",
            "documentation" to mapOf(
                "swagger" to "/swagger-ui/index.html",
                "openapi" to "/v3/api-docs",
                "postman" to "/api/v1/docs/postman",
            ),
            "endpoints" to mapOf(
                "companies" to "/api/v1/companies",
                "directors" to "/api/v1/directors",
                "shareholders" to "/api/v1/shareholders",
                "documents" to "/api/v1/documents",
                "search" to "/api/v1/search",
            ),
            "authentication" to mapOf(
                "type" to "Bearer JWT",
                "header" to "Authorization",
                "format" to "Bearer {token}",
            ),
            "supportedFormats" to listOf("application/json", "multipart/form-data"),
            "rateLimit" to mapOf(
                "requests" to 1000,
                "window" to "1 hour",
            ),
        )

        return ResponseEntity.ok(info)
    }

    @GetMapping("/endpoints")
    @Operation(summary = "Get all available endpoints")
    fun getAllEndpoints(): ResponseEntity<Map<String, Any>> {
        val endpoints = mapOf(
            "companies" to mapOf(
                "GET /api/v1/companies" to "Get all active companies",
                "POST /api/v1/companies" to "Create new company",
                "GET /api/v1/companies/{id}" to "Get company by ID",
                "PUT /api/v1/companies/{id}" to "Update company",
                "DELETE /api/v1/companies/{id}" to "Delete company",
                "GET /api/v1/companies/number/{companyNumber}" to "Get company by number",
                "GET /api/v1/companies/search" to "Search companies",
                "GET /api/v1/companies/check-name" to "Check company name availability",
                "GET /api/v1/companies/check-number" to "Check company number availability",
            ),
            "directors" to mapOf(
                "POST /api/v1/directors" to "Appoint new director",
                "GET /api/v1/directors/{id}" to "Get director by ID",
                "PUT /api/v1/directors/{id}" to "Update director",
                "DELETE /api/v1/directors/{id}" to "Delete director",
                "POST /api/v1/directors/{id}/resign" to "Resign director",
                "POST /api/v1/directors/{id}/consent" to "Give director consent",
                "POST /api/v1/directors/{id}/disqualify" to "Disqualify director",
                "GET /api/v1/directors/company/{companyId}" to "Get directors for company",
                "GET /api/v1/directors/company/{companyId}/active" to "Get active directors",
                "GET /api/v1/directors/search" to "Search directors",
            ),
            "shareholders" to mapOf(
                "POST /api/v1/shareholders" to "Create new shareholder",
                "GET /api/v1/shareholders/{id}" to "Get shareholder by ID",
                "PUT /api/v1/shareholders/{id}" to "Update shareholder",
                "DELETE /api/v1/shareholders/{id}" to "Delete shareholder",
                "GET /api/v1/shareholders/company/{companyId}" to "Get shareholders for company",
                "GET /api/v1/shareholders/company/{companyId}/individual" to "Get individual shareholders",
                "GET /api/v1/shareholders/company/{companyId}/corporate" to "Get corporate shareholders",
                "GET /api/v1/shareholders/search" to "Search shareholders",
            ),
            "documents" to mapOf(
                "POST /api/v1/documents" to "Upload document",
                "GET /api/v1/documents/{id}" to "Get document metadata",
                "GET /api/v1/documents/{id}/download" to "Download document",
                "PUT /api/v1/documents/{id}" to "Update document metadata",
                "DELETE /api/v1/documents/{id}" to "Delete document",
                "GET /api/v1/documents/company/{companyId}" to "Get documents for company",
                "GET /api/v1/documents/search" to "Search documents",
            ),
            "search" to mapOf(
                "GET /api/v1/search/global" to "Global search across all entities",
                "GET /api/v1/search/companies" to "Search companies with filters",
                "GET /api/v1/search/directors" to "Search directors with filters",
                "GET /api/v1/search/shareholders" to "Search shareholders with filters",
                "GET /api/v1/search/documents" to "Search documents with filters",
                "GET /api/v1/search/advanced" to "Advanced search with complex filters",
                "GET /api/v1/search/suggestions" to "Get search suggestions",
            ),
        )

        return ResponseEntity.ok(endpoints)
    }

    @GetMapping("/examples")
    @Operation(summary = "Get API usage examples")
    fun getApiExamples(): ResponseEntity<Map<String, Any>> {
        val examples = mapOf(
            "createCompany" to mapOf(
                "method" to "POST",
                "url" to "/api/v1/companies",
                "headers" to mapOf(
                    "Authorization" to "Bearer {token}",
                    "Content-Type" to "application/json",
                ),
                "body" to mapOf(
                    "name" to "Example Company Limited",
                    "companyNumber" to "1234567",
                    "incorporationDate" to "2024-01-15",
                    "status" to "ACTIVE",
                    "registeredOfficeAddress" to mapOf(
                        "addressLine1" to "123 Main Street",
                        "city" to "Auckland",
                        "postcode" to "1010",
                        "country" to "New Zealand",
                    ),
                ),
            ),
            "searchCompanies" to mapOf(
                "method" to "GET",
                "url" to "/api/v1/companies/search?query=Example&status=ACTIVE&limit=10",
                "headers" to mapOf(
                    "Accept" to "application/json",
                ),
            ),
            "appointDirector" to mapOf(
                "method" to "POST",
                "url" to "/api/v1/directors",
                "headers" to mapOf(
                    "Authorization" to "Bearer {token}",
                    "Content-Type" to "application/json",
                ),
                "body" to mapOf(
                    "fullName" to "John Smith",
                    "dateOfBirth" to "1980-05-15",
                    "appointmentDate" to "2024-01-15",
                    "isNzResident" to true,
                    "company" to mapOf("id" to 1),
                ),
            ),
            "uploadDocument" to mapOf(
                "method" to "POST",
                "url" to "/api/v1/documents",
                "headers" to mapOf(
                    "Authorization" to "Bearer {token}",
                    "Content-Type" to "multipart/form-data",
                ),
                "formData" to mapOf(
                    "file" to "constitution.pdf",
                    "companyId" to "1",
                    "documentType" to "CONSTITUTION",
                    "description" to "Company constitution document",
                ),
            ),
        )

        return ResponseEntity.ok(examples)
    }

    @GetMapping("/postman")
    @Operation(summary = "Get Postman collection")
    fun getPostmanCollection(): ResponseEntity<Map<String, Any>> {
        val collection = mapOf(
            "info" to mapOf(
                "name" to "NZ Companies Register API",
                "description" to "Complete API collection for NZ Companies Register",
                "version" to "1.0.0",
            ),
            "variable" to listOf(
                mapOf(
                    "key" to "baseUrl",
                    "value" to "http://localhost:8080",
                    "type" to "string",
                ),
                mapOf(
                    "key" to "token",
                    "value" to "your-jwt-token-here",
                    "type" to "string",
                ),
            ),
            "auth" to mapOf(
                "type" to "bearer",
                "bearer" to listOf(
                    mapOf(
                        "key" to "token",
                        "value" to "{{token}}",
                        "type" to "string",
                    ),
                ),
            ),
            "note" to "Import this collection into Postman to start testing the API endpoints",
        )

        return ResponseEntity.ok(collection)
    }

    @GetMapping("/status")
    @Operation(summary = "Get API status")
    fun getApiStatus(): ResponseEntity<Map<String, Any>> {
        val status = mapOf(
            "status" to "healthy",
            "timestamp" to System.currentTimeMillis(),
            "version" to "1.0.0",
            "uptime" to "Available since application startup",
            "features" to mapOf(
                "companies" to "available",
                "directors" to "available",
                "shareholders" to "available",
                "documents" to "available",
                "search" to "available",
                "authentication" to "available",
            ),
        )

        return ResponseEntity.ok(status)
    }
}
