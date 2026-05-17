package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.YearMonth
import java.util.*

interface IBudgetRepository {
    /** Returns budgets with amount_spent computed by summing EXPENSE transactions within the given month. */
    fun fetchAllWithSpentForMonth(authenticatedUser: UserDTO, yearMonth: YearMonth): List<BudgetDTO>
    fun fetchById(id: UUID, authenticatedUser: UserDTO): BudgetDTO
    fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO
    fun update(id: UUID, form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO
    fun delete(id: UUID, authenticatedUser: UserDTO)
}
