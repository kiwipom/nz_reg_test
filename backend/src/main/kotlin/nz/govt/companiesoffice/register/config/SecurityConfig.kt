package nz.govt.companiesoffice.register.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Value("\${auth0.audience}")
    private lateinit var audience: String

    @Value("\${auth0.domain}")
    private lateinit var domain: String

    @Value("\${auth0.roles-namespace}")
    private lateinit var rolesNamespace: String

    @Value("\${test.security.enabled:false}")
    private var testSecurityEnabled: Boolean = false

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authz ->
                authz
                    // Public endpoints - Always accessible (even with security enabled)
                    // Companies register public access endpoints
                    .requestMatchers(
                        "/v1/companies/search",
                        "/v1/companies/check-name",
                        "/v1/companies/check-number",
                        "/v1/companies",
                        "/v1/companies/{id}",
                        "/v1/companies/number/{companyNumber}",
                    ).permitAll()
                    // Public shareholder endpoints
                    .requestMatchers(
                        "/v1/shareholders/{id}",
                        "/v1/shareholders/company/{companyId}",
                        "/v1/shareholders/company/{companyId}/individual",
                        "/v1/shareholders/company/{companyId}/corporate",
                        "/v1/shareholders/company/{companyId}/by-location",
                        "/v1/shareholders/company/{companyId}/by-country",
                        "/v1/shareholders/company/{companyId}/by-region",
                        "/v1/shareholders/company/{companyId}/by-postcode",
                        "/v1/shareholders/search",
                        "/v1/shareholders/company/{companyId}/search-address",
                        "/v1/shareholders/company/{companyId}/statistics",
                    ).permitAll()
                    // Public share allocation endpoints
                    .requestMatchers(
                        "/v1/share-allocations/{allocationId}",
                        "/v1/share-allocations/company/{companyId}",
                        "/v1/share-allocations/shareholder/{shareholderId}",
                        "/v1/share-allocations/company/{companyId}/share-class/{shareClass}",
                        "/v1/share-allocations/company/{companyId}/statistics",
                        "/v1/share-allocations/shareholder/{shareholderId}/portfolio",
                    ).permitAll()
                    // System endpoints
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    // All other endpoints require authentication when security is enabled
                    .also { matcher ->
                        if (testSecurityEnabled) {
                            matcher.anyRequest().authenticated()
                        } else {
                            matcher.anyRequest().permitAll()
                        }
                    }
            }
            .also { httpSecurity ->
                if (testSecurityEnabled) {
                    httpSecurity.oauth2ResourceServer { oauth2 ->
                        oauth2.jwt { jwt ->
                            jwt.decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        }
                        oauth2.authenticationEntryPoint { _, response, _ ->
                            response.status = 401
                            response.contentType = "application/json"
                            response.writer.write("""{"error":"Unauthorized","message":"Authentication required"}""")
                        }
                        oauth2.accessDeniedHandler { _, response, _ ->
                            response.status = 403
                            response.contentType = "application/json"
                            response.writer.write("""{"error":"Forbidden","message":"Access denied"}""")
                        }
                    }
                }
            }
            .build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder
            .withJwkSetUri("https://$domain/.well-known/jwks.json")
            .build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        val authoritiesConverter = JwtGrantedAuthoritiesConverter()

        // Configure to extract roles from custom claim
        authoritiesConverter.setAuthoritiesClaimName("${rolesNamespace}roles")
        authoritiesConverter.setAuthorityPrefix("ROLE_")

        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter)
        return converter
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.exposedHeaders = listOf("Authorization")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
