package beer.thierry.centsible.api.model.reports

import java.math.BigDecimal

data class YearOverYearDTO(
    val thisYear: Int,
    val lastYear: Int,
    val totals: YearOverYearTotalsDTO,
    val perCategory: List<YearOverYearCategoryDTO>,
)

data class YearOverYearTotalsDTO(
    val thisYearIncome: BigDecimal,
    val thisYearExpense: BigDecimal,
    val lastYearIncome: BigDecimal,
    val lastYearExpense: BigDecimal,
)

data class YearOverYearCategoryDTO(
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String?,
    val thisYearAmount: BigDecimal,
    val lastYearAmount: BigDecimal,
)
