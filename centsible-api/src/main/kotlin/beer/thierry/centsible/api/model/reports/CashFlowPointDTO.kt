package beer.thierry.centsible.api.model.reports

import java.math.BigDecimal

data class CashFlowPointDTO(
    val yearMonth: String,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal,
)
