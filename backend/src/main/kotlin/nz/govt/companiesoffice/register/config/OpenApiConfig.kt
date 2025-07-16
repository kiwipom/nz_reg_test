package nz.govt.companiesoffice.register.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
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
                    .description("API for managing company registrations and compliance under the Companies Act 1993")
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
                    Server().url("http://localhost:8080/api").description("Local development server"),
                    Server().url("https://api.companies.govt.nz").description("Production server"),
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
