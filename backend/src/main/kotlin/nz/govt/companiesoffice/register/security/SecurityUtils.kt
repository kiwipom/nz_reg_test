package nz.govt.companiesoffice.register.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt

object SecurityUtils {
    
    fun getCurrentAuthentication(): Authentication? {
        return SecurityContextHolder.getContext().authentication
    }
    
    fun getCurrentUserSubject(): String? {
        val authentication = getCurrentAuthentication()
        return when (val principal = authentication?.principal) {
            is Jwt -> principal.subject
            else -> null
        }
    }
    
    fun getCurrentUserEmail(): String? {
        val authentication = getCurrentAuthentication()
        return when (val principal = authentication?.principal) {
            is Jwt -> principal.getClaimAsString("email")
            else -> null
        }
    }
    
    fun getCurrentUserRoles(): List<UserRole> {
        val authentication = getCurrentAuthentication()
        return when (val principal = authentication?.principal) {
            is Jwt -> {
                val rolesClaim = principal.getClaimAsStringList("https://api.companies-register.govt.nz/roles")
                rolesClaim?.mapNotNull { UserRole.fromString(it) } ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    fun hasRole(role: UserRole): Boolean {
        return getCurrentUserRoles().contains(role)
    }
    
    fun hasAnyRole(roles: List<UserRole>): Boolean {
        val userRoles = getCurrentUserRoles()
        return roles.any { userRoles.contains(it) }
    }
    
    fun hasManagementAccess(): Boolean {
        return hasAnyRole(UserRole.getManagementRoles())
    }
    
    fun hasInternalAccess(): Boolean {
        return hasAnyRole(UserRole.getInternalRoles())
    }
    
    fun canAccessCompanyData(): Boolean {
        return hasInternalAccess() || hasRole(UserRole.PUBLIC)
    }
    
    fun canModifyCompanyData(): Boolean {
        return hasManagementAccess()
    }
}