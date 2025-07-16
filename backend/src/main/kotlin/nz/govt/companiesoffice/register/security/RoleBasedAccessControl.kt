package nz.govt.companiesoffice.register.security

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
class RoleBasedAccessControl {

    @PreAuthorize("hasRole('ADMIN')")
    fun requireAdminRole() {
        // Method to enforce admin access
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR')")
    fun requireManagementRole() {
        // Method to enforce management access
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR') or hasRole('INTERNAL_OPS')")
    fun requireInternalRole() {
        // Method to enforce internal access
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('REGISTRAR') or hasRole('INTERNAL_OPS') or hasRole('PUBLIC')")
    fun requireAuthenticatedUser() {
        // Method to enforce authenticated access
    }

    fun checkCompanyAccess(companyId: Long): Boolean {
        // Additional business logic for company-specific access
        return SecurityUtils.canAccessCompanyData()
    }

    fun checkCompanyModificationAccess(companyId: Long): Boolean {
        // Additional business logic for company modification access
        return SecurityUtils.canModifyCompanyData()
    }
}
