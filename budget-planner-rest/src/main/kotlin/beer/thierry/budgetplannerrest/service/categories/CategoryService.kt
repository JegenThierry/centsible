package beer.thierry.budgetplannerrest.service.categories

import beer.thierry.budgetplannerrest.model.category.CategoryDTO
import beer.thierry.budgetplannerrest.model.category.CategoryForm
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.repository.categories.ICategoriesRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(private val categoriesRepository: ICategoriesRepository) : ICategoryService {
    override fun fetchAllCategories(authenticatedUser: UserDTO): List<CategoryDTO> {
        return categoriesRepository.fetchAllCategories(authenticatedUser)
    }

    override fun fetchCategoryById(authenticatedUser: UserDTO, id: Long): CategoryDTO? {
        return categoriesRepository.fetchCategoryById(authenticatedUser, id)
    }

    override fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO {
        return categoriesRepository.createCategory(authenticatedUser, category)
    }

    override fun updateCategory(authenticatedUser: UserDTO, id: Long, category: CategoryForm): CategoryDTO? {
        val existing = categoriesRepository.fetchCategoryById(authenticatedUser, id) ?: return null
        if (existing.type != category.type && categoriesRepository.isCategoryUsed(id)) {
            throw IllegalArgumentException("Cannot change type of a category that is referenced in transactions")
        }
        return categoriesRepository.updateCategory(authenticatedUser, id, category)
    }

    override fun deleteCategory(authenticatedUser: UserDTO, id: Long): Boolean {
        if (categoriesRepository.isCategoryUsed(id)) {
            throw IllegalArgumentException("Cannot delete a category that is referenced in transactions")
        }
        return categoriesRepository.deleteCategory(authenticatedUser, id)
    }
}
