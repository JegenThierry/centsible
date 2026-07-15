package beer.thierry.centsible.api.services.currency

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.currency.ConversionRequest
import beer.thierry.centsible.api.model.currency.ConversionResult
import java.math.BigDecimal
import java.time.LocalDate

interface ICurrencyConversionService {
    /** Converts [amount] using the [from]→[to] rate as of [date]; the result carries the rate, its date, and a same-currency flag. */
    fun convert(amount: BigDecimal, from: Currency, to: Currency, date: LocalDate): ConversionResult

    /** Batch form of [convert], results positionally matching [requests]. Call outside a write transaction: resolving a rate can hit the network. */
    fun convertAll(requests: List<ConversionRequest>, to: Currency): List<ConversionResult>
}
