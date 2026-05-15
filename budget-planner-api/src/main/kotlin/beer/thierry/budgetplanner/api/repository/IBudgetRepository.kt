package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.budget.BudgetDTO
import beer.thierry.budgetplanner.api.model.budget.BudgetForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
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
