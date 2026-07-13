package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.admin.AdminUserDTO

/**
 * Cross-user queries for the admin area. Deliberately NOT user-scoped — every other repository
 * filters by user_id; any query that must see all users lives here so the exception stays visible.
 */
interface IAdminRepository {
    /** Every user with coarse per-user counts, newest first. Metadata only — no financial data. */
    fun fetchAllUsers(): List<AdminUserDTO>
}
