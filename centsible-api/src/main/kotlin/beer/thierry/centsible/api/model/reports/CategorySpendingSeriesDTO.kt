package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.budgetaccount.Currency
import java.math.BigDecimal

data class CategorySpendingSeriesDTO(
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String?,
    val totals: List<MonthlyCategoryAmountDTO>,
)

data class MonthlyCategoryAmountDTO(
    val yearMonth: String,
    val amount: BigDecimal,
)

/**
 * One category's spend for one month on the accounts of a single currency, still in that currency.
 * Mirrors [CashFlowCurrencyPointDTO]: the aggregate carries its currency out of SQL so the caller can
 * convert before folding the rows into a [CategorySpendingSeriesDTO].
 */
data class CategorySpendingCurrencyPointDTO(
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String?,
    val yearMonth: String,
    val currency: Currency,
    val amount: BigDecimal,
)
