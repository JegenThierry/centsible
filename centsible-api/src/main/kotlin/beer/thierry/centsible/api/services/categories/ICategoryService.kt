package beer.thierry.centsible.api.services.categories

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryForm
import beer.thierry.centsible.api.model.user.UserDTO

interface ICategoryService {
    fun fetchAllCategories(authenticatedUser: UserDTO): List<CategoryDTO>
    fun fetchCategoryById(authenticatedUser: UserDTO, id: Long): CategoryDTO?
    fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO
    fun updateCategory(authenticatedUser: UserDTO, id: Long, category: CategoryForm): CategoryDTO?
    fun deleteCategory(authenticatedUser: UserDTO, id: Long): Boolean
}
