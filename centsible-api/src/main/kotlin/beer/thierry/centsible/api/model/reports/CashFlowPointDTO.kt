package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.budgetaccount.Currency
import java.math.BigDecimal

data class CashFlowPointDTO(
    val yearMonth: String,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal,
)

/**
 * One month's income/expense for the accounts of a single currency, still in that currency.
 * TRANSACTIONS.AMOUNT is stored in its account's own currency, so aggregates carry the currency
 * dimension out of SQL and are converted into the user's default currency before being summed.
 */
data class CashFlowCurrencyPointDTO(
    val yearMonth: String,
    val currency: Currency,
    val income: BigDecimal,
    val expense: BigDecimal,
)
