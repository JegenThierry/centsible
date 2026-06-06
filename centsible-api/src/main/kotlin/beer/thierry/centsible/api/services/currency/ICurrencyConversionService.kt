package beer.thierry.centsible.api.services.currency

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.currency.ConversionResult
import java.math.BigDecimal
import java.time.LocalDate

interface ICurrencyConversionService {
    fun convert(amount: BigDecimal, from: Currency, to: Currency, date: LocalDate): ConversionResult
}
