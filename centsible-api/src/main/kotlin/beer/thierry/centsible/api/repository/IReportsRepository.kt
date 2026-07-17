package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.reports.CashFlowCurrencyPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingCurrencyPointDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.LocalDate
import java.time.OffsetDateTime

interface IReportsRepository {
    fun fetchUserSnapshotsBetween(
        from: OffsetDateTime,
        until: OffsetDateTime,
        authenticatedUser: UserDTO,
    ): List<BudgetAccountSnapshotDTO>

    fun fetchLatestSnapshotPerAccountAsOf(
        until: OffsetDateTime,
        authenticatedUser: UserDTO,
    ): List<BudgetAccountSnapshotDTO>

    /** Monthly per-category spend per account currency; the caller converts and folds into a series. */
    fun fetchCategorySpendingOverTime(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<CategorySpendingCurrencyPointDTO>

    /** Monthly income/expense per account currency; the caller converts and folds (see [CashFlowCurrencyPointDTO]). */
    fun fetchCashFlow(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<CashFlowCurrencyPointDTO>
}
