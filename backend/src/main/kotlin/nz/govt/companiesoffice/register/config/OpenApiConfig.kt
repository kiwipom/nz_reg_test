package nz.govt.companiesoffice.register.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("NZ Companies Register API")
                    .description(
                        """
                        ## NZ Companies Register API
                        
                        This API provides comprehensive access to company registration and management 
                        services in accordance with the Companies Act 1993.
                        
                        ### Key Features:
                        - **Company Management**: Create, update, and manage company registrations
                        - **Director Management**: Handle director appointments, resignations, and compliance
                        - **Shareholder Management**: Manage shareholder information and shareholdings
                        - **Document Management**: Upload, store, and retrieve company documents
                        - **Search & Discovery**: Advanced search across all company data
                        - **Compliance Tracking**: Monitor compliance with statutory requirements
                        
                        ### Authentication
                        Most operations require authentication using a Bearer JWT token. 
                        Public search operations are available without authentication.
                        
                        ### Rate Limiting
                        API calls are rate-limited to ensure fair usage and system stability.
                        
                        ### Data Format
                        All API responses are in JSON format with consistent error handling.
                        """.trimIndent(),
                    )
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Companies Office")
                            .url("https://companies.govt.nz")
                            .email("info@companies.govt.nz"),
                    )
                    .license(
                        License()
                            .name("Crown Copyright")
                            .url("https://www.govt.nz/about/copyright/"),
                    ),
            )
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("Local development server"),
                    Server().url("https://api.companies.govt.nz").description("Production server"),
                    Server().url("https://test-api.companies.govt.nz").description("Test environment"),
                ),
            )
            .tags(
                listOf(
                    Tag()
                        .name("Companies")
                        .description("Company registration and management operations"),
                    Tag()
                        .name("Directors")
                        .description("Director appointment, resignation, and compliance management"),
                    Tag()
                        .name("Shareholders")
                        .description("Shareholder information and shareholding management"),
                    Tag()
                        .name("Documents")
                        .description("Document upload, storage, and retrieval operations"),
                    Tag()
                        .name("Search")
                        .description("Advanced search and discovery across all entities"),
                ),
            )
            .addSecurityItem(SecurityRequirement().addList("Bearer Authentication"))
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "Bearer Authentication",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT token obtained from authentication endpoint"),
                    ),
            )
    }
}
