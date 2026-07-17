package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.budgetaccount.Currency
import java.math.BigDecimal

data class CashFlowPointDTO(
    val yearMonth: String,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal,
)

data class CashFlowCurrencyPointDTO(
    val yearMonth: String,
    val currency: Currency,
    val income: BigDecimal,
    val expense: BigDecimal,
)
