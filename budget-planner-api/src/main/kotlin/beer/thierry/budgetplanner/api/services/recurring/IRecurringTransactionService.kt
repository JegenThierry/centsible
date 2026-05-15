package beer.thierry.budgetplanner.api.services.recurring

import beer.thierry.budgetplanner.api.model.recurring.RecurringTransactionDTO
import beer.thierry.budgetplanner.api.model.recurring.RecurringTransactionForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.util.*

interface IRecurringTransactionService {
    fun fetchAll(authenticatedUser: UserDTO, accountId: UUID? = null): List<RecurringTransactionDTO>
    fun create(accountId: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO): RecurringTransactionDTO
    fun update(id: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO): RecurringTransactionDTO
    fun delete(id: UUID, authenticatedUser: UserDTO)
    fun setActive(id: UUID, active: Boolean, authenticatedUser: UserDTO): RecurringTransactionDTO

    /** Runs the catch-up materialization pass — invoked by the scheduler. */
    fun runMaterializationPass(): Int
}
