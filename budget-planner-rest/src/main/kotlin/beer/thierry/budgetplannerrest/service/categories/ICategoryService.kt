package beer.thierry.budgetplannerrest.service.categories

import beer.thierry.budgetplannerrest.model.category.CategoryDTO
import beer.thierry.budgetplannerrest.model.category.CategoryForm
import beer.thierry.budgetplannerrest.model.user.UserDTO

interface ICategoryService {
    fun fetchAllCategories(authenticatedUser: UserDTO): List<CategoryDTO>
    fun fetchCategoryById(authenticatedUser: UserDTO, id: Long): CategoryDTO?
    fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO
    fun updateCategory(authenticatedUser: UserDTO, id: Long, category: CategoryForm): CategoryDTO?
    fun deleteCategory(authenticatedUser: UserDTO, id: Long): Boolean
}
