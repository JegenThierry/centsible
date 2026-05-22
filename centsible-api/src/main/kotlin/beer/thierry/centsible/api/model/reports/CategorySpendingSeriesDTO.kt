package beer.thierry.centsible.api.model.reports

import java.math.BigDecimal

data class CategorySpendingSeriesDTO(
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String?,
    val totals: List<MonthlyCategoryAmountDTO>,
)

data class MonthlyCategoryAmountDTO(
    val yearMonth: String, // YYYY-MM
    val amount: BigDecimal,
)
