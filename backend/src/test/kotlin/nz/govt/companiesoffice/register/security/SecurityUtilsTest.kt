package nz.govt.companiesoffice.register.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecurityUtilsTest {

    private val mockAuthentication: Authentication = mockk()
    private val mockSecurityContext: SecurityContext = mockk()
    private val mockJwt: Jwt = mockk()

    @BeforeEach
    fun setUp() {
        mockkObject(SecurityContextHolder)
        every { SecurityContextHolder.getContext() } returns mockSecurityContext
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SecurityContextHolder)
    }

    @Nested
    @DisplayName("Authentication Tests")
    inner class AuthenticationTests {

        @Test
        @DisplayName("Should return current authentication when available")
        fun `getCurrentAuthentication should return authentication when available`() {
            // Given
            every { mockSecurityContext.authentication } returns mockAuthentication

            // When
            val result = SecurityUtils.getCurrentAuthentication()

            // Then
            assertEquals(mockAuthentication, result)
        }

        @Test
        @DisplayName("Should return null when no authentication available")
        fun `getCurrentAuthentication should return null when not available`() {
            // Given
            every { mockSecurityContext.authentication } returns null

            // When
            val result = SecurityUtils.getCurrentAuthentication()

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("User Information Tests")
    inner class UserInformationTests {

        @Test
        @DisplayName("Should return user subject from JWT token")
        fun `getCurrentUserSubject should return subject from JWT`() {
            // Given
            val expectedSubject = "auth0|user123"
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.subject } returns expectedSubject

            // When
            val result = SecurityUtils.getCurrentUserSubject()

            // Then
            assertEquals(expectedSubject, result)
        }

        @Test
        @DisplayName("Should return null when no JWT principal")
        fun `getCurrentUserSubject should return null when no JWT principal`() {
            // Given
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns "not-a-jwt"

            // When
            val result = SecurityUtils.getCurrentUserSubject()

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should return null when no authentication")
        fun `getCurrentUserSubject should return null when no authentication`() {
            // Given
            every { mockSecurityContext.authentication } returns null

            // When
            val result = SecurityUtils.getCurrentUserSubject()

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should return user email from JWT token")
        fun `getCurrentUserEmail should return email from JWT`() {
            // Given
            val expectedEmail = "user@example.com"
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsString("email") } returns expectedEmail

            // When
            val result = SecurityUtils.getCurrentUserEmail()

            // Then
            assertEquals(expectedEmail, result)
        }

        @Test
        @DisplayName("Should return null when no email claim")
        fun `getCurrentUserEmail should return null when no email claim`() {
            // Given
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsString("email") } returns null

            // When
            val result = SecurityUtils.getCurrentUserEmail()

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("Role Tests")
    inner class RoleTests {

        @Test
        @DisplayName("Should return user roles from JWT token")
        fun `getCurrentUserRoles should return roles from JWT`() {
            // Given
            val expectedRoles = listOf("ADMIN", "REGISTRAR")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns expectedRoles

            // When
            val result = SecurityUtils.getCurrentUserRoles()

            // Then
            assertEquals(listOf(UserRole.ADMIN, UserRole.REGISTRAR), result)
        }

        @Test
        @DisplayName("Should return empty list when no roles claim")
        fun `getCurrentUserRoles should return empty list when no roles claim`() {
            // Given
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns null

            // When
            val result = SecurityUtils.getCurrentUserRoles()

            // Then
            assertEquals(emptyList(), result)
        }

        @Test
        @DisplayName("Should return empty list when no authentication")
        fun `getCurrentUserRoles should return empty list when no authentication`() {
            // Given
            every { mockSecurityContext.authentication } returns null

            // When
            val result = SecurityUtils.getCurrentUserRoles()

            // Then
            assertEquals(emptyList(), result)
        }

        @Test
        @DisplayName("Should filter out invalid roles")
        fun `getCurrentUserRoles should filter out invalid roles`() {
            // Given
            val rolesWithInvalid = listOf("ADMIN", "INVALID_ROLE", "REGISTRAR")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every {
                mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles")
            } returns rolesWithInvalid

            // When
            val result = SecurityUtils.getCurrentUserRoles()

            // Then
            assertEquals(listOf(UserRole.ADMIN, UserRole.REGISTRAR), result)
        }
    }

    @Nested
    @DisplayName("Permission Tests")
    inner class PermissionTests {

        @Test
        @DisplayName("Should return true when user has specific role")
        fun `hasRole should return true when user has role`() {
            // Given
            val userRoles = listOf("ADMIN", "REGISTRAR")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.hasRole(UserRole.ADMIN)

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return false when user does not have specific role")
        fun `hasRole should return false when user does not have role`() {
            // Given
            val userRoles = listOf("PUBLIC")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.hasRole(UserRole.ADMIN)

            // Then
            assertFalse(result)
        }

        @Test
        @DisplayName("Should return true when user has any of the specified roles")
        fun `hasAnyRole should return true when user has any role`() {
            // Given
            val userRoles = listOf("REGISTRAR")
            val requiredRoles = listOf(UserRole.ADMIN, UserRole.REGISTRAR)
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.hasAnyRole(requiredRoles)

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return false when user has none of the specified roles")
        fun `hasAnyRole should return false when user has none of the roles`() {
            // Given
            val userRoles = listOf("PUBLIC")
            val requiredRoles = listOf(UserRole.ADMIN, UserRole.REGISTRAR)
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.hasAnyRole(requiredRoles)

            // Then
            assertFalse(result)
        }

        @Test
        @DisplayName("Should return true when user has management access")
        fun `hasManagementAccess should return true for admin`() {
            // Given
            val userRoles = listOf("ADMIN")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.hasManagementAccess()

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return true when user has management access")
        fun `hasManagementAccess should return true for registrar`() {
            // Given
            val userRoles = listOf("REGISTRAR")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.hasManagementAccess()

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return false when user has no management access")
        fun `hasManagementAccess should return false for public user`() {
            // Given
            val userRoles = listOf("PUBLIC")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.hasManagementAccess()

            // Then
            assertFalse(result)
        }

        @Test
        @DisplayName("Should return true when user has internal access")
        fun `hasInternalAccess should return true for internal ops`() {
            // Given
            val userRoles = listOf("INTERNAL_OPS")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.hasInternalAccess()

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return true when user can access company data")
        fun `canAccessCompanyData should return true for public user`() {
            // Given
            val userRoles = listOf("PUBLIC")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.canAccessCompanyData()

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return false when user cannot modify company data")
        fun `canModifyCompanyData should return false for public user`() {
            // Given
            val userRoles = listOf("PUBLIC")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.canModifyCompanyData()

            // Then
            assertFalse(result)
        }

        @Test
        @DisplayName("Should return true when user can modify company data")
        fun `canModifyCompanyData should return true for admin`() {
            // Given
            val userRoles = listOf("ADMIN")
            every { mockSecurityContext.authentication } returns mockAuthentication
            every { mockAuthentication.principal } returns mockJwt
            every { mockJwt.getClaimAsStringList("https://api.companies-register.govt.nz/roles") } returns userRoles

            // When
            val result = SecurityUtils.canModifyCompanyData()

            // Then
            assertTrue(result)
        }
    }
}
