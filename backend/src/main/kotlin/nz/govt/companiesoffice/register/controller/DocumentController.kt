package nz.govt.companiesoffice.register.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import nz.govt.companiesoffice.register.entity.Document
import nz.govt.companiesoffice.register.entity.DocumentType
import nz.govt.companiesoffice.register.service.CompanyService
import nz.govt.companiesoffice.register.service.DocumentService
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/v1/documents")
@Tag(name = "Documents", description = "Document management operations")
class DocumentController(
    private val documentService: DocumentService,
    private val companyService: CompanyService,
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    @Operation(
        summary = "Upload document",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun uploadDocument(
        @RequestPart("file") file: MultipartFile,
        @RequestParam companyId: Long,
        @RequestParam documentType: String,
        @RequestParam(required = false) description: String?,
    ): ResponseEntity<Document> {
        val company = companyService.getCompanyById(companyId)
        val docType = DocumentType.valueOf(documentType)
        val result = documentService.uploadDocument(company, docType, file, description = description)
        return ResponseEntity.status(HttpStatus.CREATED).body(result.document)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata by ID")
    fun getDocumentById(@PathVariable id: Long): ResponseEntity<Document> {
        val document = documentService.getDocumentById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(document)
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document content")
    fun downloadDocument(@PathVariable id: Long): ResponseEntity<Resource> {
        val document = documentService.getDocumentById(id)
            ?: return ResponseEntity.notFound().build()
        val content = documentService.downloadDocument(id)
            ?: return ResponseEntity.notFound().build()

        val resource = ByteArrayResource(content)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(document.mimeType ?: "application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.documentName}\"")
            .body(resource)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete document",
        security = [SecurityRequirement(name = "Bearer Authentication")],
    )
    fun deleteDocument(@PathVariable id: Long): ResponseEntity<Void> {
        documentService.deleteDocument(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Get all documents for a company")
    fun getDocumentsByCompany(@PathVariable companyId: Long): ResponseEntity<List<Document>> {
        val company = companyService.getCompanyById(companyId)
        val documents = documentService.getDocumentsByCompany(company)
        return ResponseEntity.ok(documents)
    }

    @GetMapping("/company/{companyId}/type/{documentType}")
    @Operation(summary = "Get documents by company and type")
    fun getDocumentsByCompanyAndType(
        @PathVariable companyId: Long,
        @PathVariable documentType: String,
    ): ResponseEntity<List<Document>> {
        val company = companyService.getCompanyById(companyId)
        val docType = DocumentType.valueOf(documentType)
        val documents = documentService.getDocumentsByCompanyAndType(company, docType)
        return ResponseEntity.ok(documents)
    }

    @GetMapping("/company/{companyId}/public")
    @Operation(summary = "Get public documents for a company")
    fun getPublicDocumentsByCompany(@PathVariable companyId: Long): ResponseEntity<List<Document>> {
        val company = companyService.getCompanyById(companyId)
        val documents = documentService.getPublicDocumentsByCompany(company)
        return ResponseEntity.ok(documents)
    }

    @GetMapping("/company/{companyId}/latest/{documentType}")
    @Operation(summary = "Get latest document by company and type")
    fun getLatestDocumentByCompanyAndType(
        @PathVariable companyId: Long,
        @PathVariable documentType: String,
    ): ResponseEntity<Document> {
        val company = companyService.getCompanyById(companyId)
        val docType = DocumentType.valueOf(documentType)
        val document = documentService.getLatestDocumentByCompanyAndType(company, docType)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(document)
    }
}
