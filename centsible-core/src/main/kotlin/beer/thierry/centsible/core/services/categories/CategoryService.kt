package beer.thierry.centsible.core.services.categories

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.services.categories.ICategoryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CategoryService(private val categoriesRepository: ICategoriesRepository) : ICategoryService {

    private val log = LoggerFactory.getLogger(CategoryService::class.java)

    override fun fetchAllCategories(authenticatedUser: UserDTO): List<CategoryDTO> =
        categoriesRepository.fetchAllCategories(authenticatedUser)

    override fun fetchCategoryById(authenticatedUser: UserDTO, id: Long): CategoryDTO? =
        categoriesRepository.fetchCategoryById(authenticatedUser, id)

    override fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO {
        val created = categoriesRepository.createCategory(authenticatedUser, category)
        log.info(
            "Created category id={} userId={} type={}",
            created.id, authenticatedUser.id, created.type,
        )
        return created
    }

    override fun updateCategory(authenticatedUser: UserDTO, id: Long, category: CategoryForm): CategoryDTO? {
        val updated = categoriesRepository.updateCategory(authenticatedUser, id, category)
        if (updated != null) {
            log.info("Updated category id={} userId={}", id, authenticatedUser.id)
        }
        return updated
    }

    override fun deleteCategory(authenticatedUser: UserDTO, id: Long): Boolean {
        if (categoriesRepository.isCategoryUsed(authenticatedUser, id)) {
            throw IllegalArgumentException("Cannot delete a category that is referenced in transactions")
        }
        val deleted = categoriesRepository.deleteCategory(authenticatedUser, id)
        if (deleted) {
            log.info("Deleted category id={} userId={}", id, authenticatedUser.id)
        }
        return deleted
    }
}
