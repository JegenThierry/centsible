package beer.thierry.budgetplannerrest.repository.categories

import beer.thierry.budgetplannerrest.model.category.CategoryDTO
import beer.thierry.budgetplannerrest.model.category.CategoryForm
import beer.thierry.budgetplannerrest.model.user.UserDTO

interface ICategoriesRepository {
    fun fetchAllCategories(authenticatedUser: UserDTO) : List<CategoryDTO>
    fun fetchCategoryById(authenticatedUser: UserDTO) : List<CategoryDTO>
    fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO
}
