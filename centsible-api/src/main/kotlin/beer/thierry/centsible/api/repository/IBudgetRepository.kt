package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.budget.BudgetPeriodType
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
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

    /**
     * Average monthly spend per category over the [months] full calendar months ending the month before
     * [asOf], as a budget-amount suggestion. Reuses the same transfer-excluded, split-aware expense sum as
     * budget "spent". Categories with no spend are absent from the result.
     */
    fun suggestedAmounts(
        authenticatedUser: UserDTO,
        categoryIds: List<Long>,
        months: Int,
        asOf: YearMonth,
    ): Map<Long, BigDecimal>

    /** The category ids the user already has a [periodType] budget for, so bulk-create can skip them in one query. */
    fun budgetedCategoryIds(authenticatedUser: UserDTO, periodType: BudgetPeriodType): Set<Long>

    fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO
    fun update(id: UUID, form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO
    fun delete(id: UUID, authenticatedUser: UserDTO)
}
