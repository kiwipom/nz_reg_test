package nz.govt.companiesoffice.register.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import java.time.Instant

@TestConfiguration
class TestSecurityConfig {

    @Bean
    @Primary
    fun testJwtDecoder(): JwtDecoder {
        return JwtDecoder { token ->
            Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .header("typ", "JWT")
                .claim("sub", "test-user")
                .claim("aud", "https://test.api.nz")
                .claim("iss", "https://test.auth0.com/")
                .claim("https://test.api.nz/roles", listOf("ADMIN"))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        }
    }
}
