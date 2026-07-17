package beer.thierry.centsible.api.model.loan

import java.math.BigDecimal

data class OutstandingTotalDTO(
    val outstanding: BigDecimal,
    val excludedCount: Int,
)
