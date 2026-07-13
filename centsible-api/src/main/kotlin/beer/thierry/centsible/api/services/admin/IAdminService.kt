package beer.thierry.centsible.api.services.admin

import beer.thierry.centsible.api.model.admin.AdminUserDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

/**
 * Instance-level user management for the configured admin. Every operation re-checks the caller
 * against [IAdminAccessService]; the ROLE_ADMIN path rule in the REST layer is defense in depth,
 * not the authorization.
 */
interface IAdminService {
    /** All users with coarse per-user counts (metadata only). Caller must be the admin. */
    fun listUsers(authenticatedUser: UserDTO): List<AdminUserDTO>

    /** Hard-deletes [userId] and all their data (DB cascade). The admin cannot delete themselves. */
    fun deleteUser(authenticatedUser: UserDTO, userId: UUID)

    /** Resends the verification email for an unconfirmed account (fresh token, old one replaced). */
    fun resendVerificationEmail(authenticatedUser: UserDTO, userId: UUID)
}
