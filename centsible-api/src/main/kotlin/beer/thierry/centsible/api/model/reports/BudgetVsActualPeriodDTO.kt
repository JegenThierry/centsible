package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.budget.BudgetPeriodType
import java.math.BigDecimal

data class BudgetVsActualPeriodDTO(
    val periodKey: String,
    val periodType: BudgetPeriodType,
    val entries: List<BudgetVsActualEntryDTO>,
)

data class BudgetVsActualEntryDTO(
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String?,
    val limit: BigDecimal,
    val spent: BigDecimal,
)
