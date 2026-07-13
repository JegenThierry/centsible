package beer.thierry.centsible.api.services.admin

/**
 * Answers "is this the instance admin?" from configuration alone: the feature toggle
 * (admin.enabled / ADMIN_ENABLED) and the designated username (admin.username / ADMIN_USERNAME).
 * Kept free of other dependencies so the JWT filter and user mapping can inject it without cycles.
 */
interface IAdminAccessService {
    /** True when the admin feature is switched on for this instance. */
    fun isEnabled(): Boolean

    /** True when the feature is enabled and [username] is the configured admin. */
    fun isAdmin(username: String): Boolean
}
