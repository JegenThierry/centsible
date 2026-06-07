package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.budget.BudgetPeriodType
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.YearMonth
import java.util.*

interface IBudgetRepository {
    /** Returns budgets with amount_spent computed by summing EXPENSE transactions within the given month. */
    fun fetchAllWithSpentForMonth(authenticatedUser: UserDTO, yearMonth: YearMonth): List<BudgetDTO>
    fun fetchById(id: UUID, authenticatedUser: UserDTO): BudgetDTO

    /**
     * True when [authenticatedUser] already has a budget for [categoryId] in [periodType], optionally
     * excluding [excludeBudgetId] (so an update can ignore the row it is editing). Mirrors the
     * uq_budgets_user_category_period unique constraint for a friendly pre-insert check.
     */
    fun existsForCategoryAndPeriod(
        authenticatedUser: UserDTO,
        categoryId: Long,
        periodType: BudgetPeriodType,
        excludeBudgetId: UUID? = null,
    ): Boolean

    fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO
    fun update(id: UUID, form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO
    fun delete(id: UUID, authenticatedUser: UserDTO)
}
