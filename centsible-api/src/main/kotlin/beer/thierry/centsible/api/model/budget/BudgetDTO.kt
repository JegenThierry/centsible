package beer.thierry.centsible.api.model.budget

import beer.thierry.centsible.api.model.category.CategoryDTO
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.*

enum class BudgetPeriodType { MONTHLY, QUARTERLY, ANNUAL }

data class BudgetDTO(
    var id: UUID? = null,
    var category: CategoryDTO = CategoryDTO(),
    var amountLimit: BigDecimal = BigDecimal.ZERO,
    var amountSpent: BigDecimal = BigDecimal.ZERO,
    var period: String? = null, // YYYY-MM (or window key)
    var periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,
    var rolloverEnabled: Boolean = false,
    var rolloverAmount: BigDecimal = BigDecimal.ZERO,
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null,
) {
    companion object {
        fun periodKey(yearMonth: YearMonth): String = yearMonth.toString()
    }
}
