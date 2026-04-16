package beer.thierry.budgetplanner.api.services.categories

import beer.thierry.budgetplanner.api.model.category.CategoryDTO
import beer.thierry.budgetplanner.api.model.category.CategoryForm
import beer.thierry.budgetplanner.api.model.user.UserDTO

interface ICategoryService {
    fun fetchAllCategories(authenticatedUser: UserDTO): List<CategoryDTO>
    fun fetchCategoryById(authenticatedUser: UserDTO, id: Long): CategoryDTO?
    fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO
    fun updateCategory(authenticatedUser: UserDTO, id: Long, category: CategoryForm): CategoryDTO?
    fun deleteCategory(authenticatedUser: UserDTO, id: Long): Boolean
}
