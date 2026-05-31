package beer.thierry.centsible.core.services.recurring

import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.recurring.RecurringTransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.services.recurring.IRecurringTransactionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class RecurringTransactionService(
    private val repository: IRecurringTransactionRepository,
    private val categoriesRepository: ICategoriesRepository,
) : IRecurringTransactionService {

    private val log = LoggerFactory.getLogger(RecurringTransactionService::class.java)

    override fun fetchAll(authenticatedUser: UserDTO, accountId: UUID?): List<RecurringTransactionDTO> =
        repository.fetchAll(authenticatedUser, accountId)

    override fun create(
        accountId: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO
    ): RecurringTransactionDTO {
        validate(form)
        assertCategoryOwned(authenticatedUser, form.categoryId)
        val created = repository.create(accountId, form, authenticatedUser)
        log.info(
            "Created recurring transaction id={} accountId={} userId={} frequency={}",
            created.id, accountId, authenticatedUser.id, form.frequency,
        )
        return created
    }

    override fun update(
        id: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO
    ): RecurringTransactionDTO {
        validate(form)
        assertCategoryOwned(authenticatedUser, form.categoryId)
        val updated = repository.update(id, form, authenticatedUser)
        log.info(
            "Updated recurring transaction id={} userId={} frequency={} active={}",
            id, authenticatedUser.id, form.frequency, form.active,
        )
        return updated
    }

    override fun delete(id: UUID, authenticatedUser: UserDTO) {
        repository.delete(id, authenticatedUser)
        log.info("Deleted recurring transaction id={} userId={}", id, authenticatedUser.id)
    }

    override fun setActive(id: UUID, active: Boolean, authenticatedUser: UserDTO): RecurringTransactionDTO {
        val current = repository.fetchById(id, authenticatedUser)
        val form = RecurringTransactionForm(
            amount = current.amount!!,
            categoryId = current.category.id!!,
            description = current.description!!,
            frequency = current.frequency!!,
            startDate = current.startDate!!,
            endDate = current.endDate,
            active = active,
        )
        val updated = repository.update(id, form, authenticatedUser)
        log.info(
            "Recurring transaction active={} id={} userId={}",
            active, id, authenticatedUser.id,
        )
        return updated
    }

    @Transactional
    override fun runMaterializationPass(): Int {
        val today = LocalDate.now()
        var count = 0
        for (rule in repository.fetchDueRules(today)) {
            var working = rule
            while (working.active && (working.nextRunAt?.let { it <= today } == true)) {
                val newNext = repository.materializeOnce(working)
                count++
                if (newNext == null || newNext > today) break
                working = working.copy(nextRunAt = newNext)
            }
        }
        if (count > 0) log.info("Materialized {} recurring transaction(s) for {}", count, today)
        return count
    }

    private fun validate(form: RecurringTransactionForm) {
        val end = form.endDate ?: return
        require(!end.isBefore(form.startDate)) { "End date must be on or after start date." }
    }

    private fun assertCategoryOwned(authenticatedUser: UserDTO, categoryId: Long) {
        if (!categoriesRepository.fetchCategoryClassifications(authenticatedUser, listOf(categoryId))
                .containsKey(categoryId)
        ) {
            throw IllegalArgumentException("Category $categoryId not found or not accessible")
        }
    }
}
