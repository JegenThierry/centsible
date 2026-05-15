package beer.thierry.budgetplanner.api.model.transaction

import java.math.BigDecimal

data class CategoryAggregateDTO(
    var categoryId: Long = 0,
    var categoryName: String = "",
    var categoryColor: String? = null,
    var categoryIcon: String? = null,
    var total: BigDecimal = BigDecimal.ZERO,
)
