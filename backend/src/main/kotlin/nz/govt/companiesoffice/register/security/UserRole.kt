package nz.govt.companiesoffice.register.security

enum class UserRole(val roleName: String, val description: String) {
    ADMIN("ADMIN", "System administrators with full access"),
    REGISTRAR("REGISTRAR", "Companies Office registrars with management access"),
    INTERNAL_OPS("INTERNAL_OPS", "Internal operations staff with limited access"),
    PUBLIC("PUBLIC", "Public users with read-only access");

    companion object {
        fun fromString(role: String): UserRole? {
            return values().find { it.roleName.equals(role, ignoreCase = true) }
        }
        
        fun getManagementRoles(): List<UserRole> {
            return listOf(ADMIN, REGISTRAR)
        }
        
        fun getInternalRoles(): List<UserRole> {
            return listOf(ADMIN, REGISTRAR, INTERNAL_OPS)
        }
    }
}