package beer.thierry.centsible.core.services.recurring

import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.recurring.RecurringTransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
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
) : IRecurringTransactionService {

    private val log = LoggerFactory.getLogger(RecurringTransactionService::class.java)

    override fun fetchAll(authenticatedUser: UserDTO, accountId: UUID?): List<RecurringTransactionDTO> =
        repository.fetchAll(authenticatedUser, accountId)

    override fun create(
        accountId: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO
    ): RecurringTransactionDTO {
        validate(form)
        return repository.create(accountId, form, authenticatedUser)
    }

    override fun update(
        id: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO
    ): RecurringTransactionDTO {
        validate(form)
        return repository.update(id, form, authenticatedUser)
    }

    override fun delete(id: UUID, authenticatedUser: UserDTO) {
        repository.delete(id, authenticatedUser)
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
        return repository.update(id, form, authenticatedUser)
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
        require(form.endDate == null || !form.endDate!!.isBefore(form.startDate)) {
            "End date must be on or after start date."
        }
    }
}
