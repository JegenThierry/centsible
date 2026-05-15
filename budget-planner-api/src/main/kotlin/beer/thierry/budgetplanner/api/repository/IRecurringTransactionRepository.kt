package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.recurring.RecurringTransactionDTO
import beer.thierry.budgetplanner.api.model.recurring.RecurringTransactionForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.time.LocalDate
import java.util.*

interface IRecurringTransactionRepository {
    fun fetchAll(authenticatedUser: UserDTO, accountId: UUID? = null): List<RecurringTransactionDTO>
    fun fetchById(id: UUID, authenticatedUser: UserDTO): RecurringTransactionDTO
    fun create(accountId: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO): RecurringTransactionDTO
    fun update(id: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO): RecurringTransactionDTO
    fun delete(id: UUID, authenticatedUser: UserDTO)

    /** Returns active rules whose next_run_at <= today and (end_date IS NULL OR end_date >= next_run_at). */
    fun fetchDueRules(today: LocalDate): List<RecurringTransactionDTO>

    /**
     * Inserts a transaction for the rule, advances next_run_at by one frequency step,
     * and adjusts the account balance — all in a single SQL transaction.
     * Returns the new next_run_at, or null if the rule was deactivated (end_date passed).
     */
    fun materializeOnce(rule: RecurringTransactionDTO): LocalDate?
}
