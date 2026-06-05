package beer.thierry.centsible.api.model.transaction

import java.math.BigDecimal

data class DailyAggregateDTO(
    val date: String = "",
    val income: BigDecimal = BigDecimal.ZERO,
    val expense: BigDecimal = BigDecimal.ZERO,
)
