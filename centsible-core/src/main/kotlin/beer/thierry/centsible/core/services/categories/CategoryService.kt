package beer.thierry.centsible.core.services.categories

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.services.categories.ICategoryService
import org.springframework.stereotype.Service

@Service
class CategoryService(private val categoriesRepository: ICategoriesRepository) : ICategoryService {
    override fun fetchAllCategories(authenticatedUser: UserDTO): List<CategoryDTO> =
        categoriesRepository.fetchAllCategories(authenticatedUser)

    override fun fetchCategoryById(authenticatedUser: UserDTO, id: Long): CategoryDTO? =
        categoriesRepository.fetchCategoryById(authenticatedUser, id)

    override fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO =
        categoriesRepository.createCategory(authenticatedUser, category)

    override fun updateCategory(authenticatedUser: UserDTO, id: Long, category: CategoryForm): CategoryDTO? {
        val existing = categoriesRepository.fetchCategoryById(authenticatedUser, id) ?: return null
        if (existing.type != category.type && categoriesRepository.isCategoryUsed(authenticatedUser, id)) {
            throw IllegalArgumentException("Cannot change type of a category that is referenced in transactions")
        }
        return categoriesRepository.updateCategory(authenticatedUser, id, category)
    }

    override fun deleteCategory(authenticatedUser: UserDTO, id: Long): Boolean {
        if (categoriesRepository.isCategoryUsed(authenticatedUser, id)) {
            throw IllegalArgumentException("Cannot delete a category that is referenced in transactions")
        }
        return categoriesRepository.deleteCategory(authenticatedUser, id)
    }
}
