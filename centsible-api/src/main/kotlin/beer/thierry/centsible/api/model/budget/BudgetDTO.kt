package beer.thierry.centsible.api.model.budget

import beer.thierry.centsible.api.model.category.CategoryDTO
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.*

data class BudgetDTO(
    var id: UUID? = null,
    var category: CategoryDTO = CategoryDTO(),
    var amountLimit: BigDecimal = BigDecimal.ZERO,
    var amountSpent: BigDecimal = BigDecimal.ZERO,
    var period: String? = null, // YYYY-MM
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null,
) {
    companion object {
        fun periodKey(yearMonth: YearMonth): String = yearMonth.toString()
    }
}
