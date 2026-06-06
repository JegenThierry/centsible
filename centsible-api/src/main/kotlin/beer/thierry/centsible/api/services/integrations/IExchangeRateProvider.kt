package beer.thierry.centsible.api.services.integrations

import beer.thierry.centsible.api.model.budgetaccount.Currency
import java.math.BigDecimal
import java.time.LocalDate

interface IExchangeRateProvider {
    val source: String get() = "external"

    fun fetchRate(base: Currency, quote: Currency, date: LocalDate): ProviderRate
}

data class ProviderRate(
    val rate: BigDecimal,
    val rateDate: LocalDate,
)
