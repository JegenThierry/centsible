package beer.thierry.budgetplanner.api.model.transaction

import java.math.BigDecimal

data class MonthlyAggregateDTO(
    var yearMonth: String = "", // YYYY-MM
    var income: BigDecimal = BigDecimal.ZERO,
    var expense: BigDecimal = BigDecimal.ZERO,
)
