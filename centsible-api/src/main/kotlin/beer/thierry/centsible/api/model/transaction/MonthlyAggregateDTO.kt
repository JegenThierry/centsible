package beer.thierry.centsible.api.model.transaction

import java.math.BigDecimal

data class MonthlyAggregateDTO(
    val yearMonth: String = "", // YYYY-MM
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
)
