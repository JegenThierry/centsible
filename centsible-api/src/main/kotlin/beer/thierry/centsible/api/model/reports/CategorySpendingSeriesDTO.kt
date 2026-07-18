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

data class CategorySpendingCurrencyPointDTO(
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String?,
    val yearMonth: String,
    val currency: Currency,
    val amount: BigDecimal,
)
