package beer.thierry.centsible.api.model.currency

import beer.thierry.centsible.api.model.budgetaccount.Currency
import java.math.BigDecimal
import java.time.LocalDate

data class ConversionResult(
    val convertedAmount: BigDecimal,
    val originalAmount: BigDecimal,
    val originalCurrency: Currency,
    val accountCurrency: Currency,
    val rate: BigDecimal,
    val rateDate: LocalDate,
    val sameCurrency: Boolean,
)
