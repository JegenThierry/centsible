package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryForm
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.user.UserDTO

interface ICategoriesRepository {
    fun fetchAllCategories(authenticatedUser: UserDTO): List<CategoryDTO>

    /** [authenticatedUser]'s or a shared system category by [id], excluding managed (Lending / Repayment) ones; null if absent. */
    fun fetchCategoryById(authenticatedUser: UserDTO, id: Long): CategoryDTO?

    /** Shared, user-agnostic system category identified by its stable [systemKey], or null. */
    fun fetchSystemCategoryByKey(systemKey: String): CategoryDTO?

    /**
     * Returns the category's default transaction type plus its managed flag.
     * Unlike [fetchCategoryById], this does NOT exclude managed categories
     * (Lending / Repayment) — the service layer needs to inspect
     * [CategoryClassification.isManaged] to force the transaction type on
     * those rows.
     */
    fun fetchCategoryClassifications(authenticatedUser: UserDTO, ids: Collection<Long>): Map<Long, CategoryClassification>

    fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO
    fun updateCategory(authenticatedUser: UserDTO, id: Long, category: CategoryForm): CategoryDTO?
    fun deleteCategory(authenticatedUser: UserDTO, id: Long): Boolean
    fun isCategoryUsed(authenticatedUser: UserDTO, id: Long): Boolean
}

data class CategoryClassification(
    val type: CategoryType,
    val isManaged: Boolean,
)
