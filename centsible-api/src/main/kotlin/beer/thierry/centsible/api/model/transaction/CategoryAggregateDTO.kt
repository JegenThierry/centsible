package beer.thierry.centsible.api.model.transaction

import java.math.BigDecimal

data class CategoryAggregateDTO(
    val categoryId: Long = 0,
    val categoryName: String = "",
    val categoryColor: String? = null,
    val categoryIcon: String? = null,
    val total: BigDecimal = BigDecimal.ZERO,
)
