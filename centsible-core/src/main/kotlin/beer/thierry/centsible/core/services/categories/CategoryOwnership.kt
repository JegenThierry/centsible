package beer.thierry.centsible.core.services.categories

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.CategoryClassification
import beer.thierry.centsible.api.repository.ICategoriesRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("beer.thierry.centsible.core.services.categories.CategoryOwnership")

/**
 * Returns the [CategoryClassification] for [categoryId] scoped to [user], or throws a localized
 * BadRequest when the category is missing or not owned. Single source for the "category not
 * accessible" guard shared by the budget, transaction, recurring and rule services (ADR-0003/0004),
 * so the check and its error stay consistent instead of being re-spelled per service.
 */
internal fun ICategoriesRepository.requireOwnedClassification(
    user: UserDTO,
    categoryId: Long,
): CategoryClassification =
    fetchCategoryClassifications(user, listOf(categoryId))[categoryId]
        ?: run {
            log.warn("Category {} not found or not accessible for user {}", categoryId, user.id)
            throw LocalizedException.BadRequest("error.category.notAccessible")
        }
