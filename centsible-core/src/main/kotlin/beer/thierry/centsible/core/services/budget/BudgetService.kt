package beer.thierry.centsible.core.services.budget

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.services.budget.IBudgetService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.*

@Service
class BudgetService(
    private val repository: IBudgetRepository,
    private val categoriesRepository: ICategoriesRepository,
) : IBudgetService {

    private val log = LoggerFactory.getLogger(BudgetService::class.java)

    override fun fetchAllForMonth(authenticatedUser: UserDTO, yearMonth: YearMonth): List<BudgetDTO> =
        repository.fetchAllWithSpentForMonth(authenticatedUser, yearMonth)

    override fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO {
        assertCategoryOwned(authenticatedUser, form.categoryId)
        assertNoDuplicate(authenticatedUser, form)
        val created = repository.create(form, authenticatedUser)
        log.info(
            "Created budget id={} userId={} categoryId={} period={} limit={}",
            created.id, authenticatedUser.id, form.categoryId, form.periodType, form.amountLimit,
        )
        return created
    }

    override fun update(id: UUID, form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO {
        assertCategoryOwned(authenticatedUser, form.categoryId)
        assertNoDuplicate(authenticatedUser, form, excludeBudgetId = id)
        val updated = repository.update(id, form, authenticatedUser)
        log.info(
            "Updated budget id={} userId={} categoryId={} limit={}",
            id, authenticatedUser.id, form.categoryId, form.amountLimit,
        )
        return updated
    }

    override fun delete(id: UUID, authenticatedUser: UserDTO) {
        repository.delete(id, authenticatedUser)
        log.info("Deleted budget id={} userId={}", id, authenticatedUser.id)
    }

    // One budget per (category, period type). The DB unique constraint is the hard guard; this gives
    // a specific localized message instead of the generic conflict the constraint violation maps to.
    private fun assertNoDuplicate(authenticatedUser: UserDTO, form: BudgetForm, excludeBudgetId: UUID? = null) {
        if (repository.existsForCategoryAndPeriod(authenticatedUser, form.categoryId, form.periodType, excludeBudgetId)) {
            log.warn(
                "Duplicate budget rejected userId={} categoryId={} period={}",
                authenticatedUser.id, form.categoryId, form.periodType,
            )
            throw LocalizedException.Conflict("error.budget.duplicate")
        }
    }

    private fun assertCategoryOwned(authenticatedUser: UserDTO, categoryId: Long) {
        if (!categoriesRepository.fetchCategoryClassifications(authenticatedUser, listOf(categoryId))
                .containsKey(categoryId)
        ) {
            log.warn("Category {} not found or not accessible", categoryId)
            throw LocalizedException.BadRequest("error.category.notAccessible")
        }
    }
}
