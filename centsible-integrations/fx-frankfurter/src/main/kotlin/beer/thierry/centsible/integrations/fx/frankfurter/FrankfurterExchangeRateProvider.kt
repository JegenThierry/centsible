package beer.thierry.centsible.integrations.fx.frankfurter

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.services.integrations.IExchangeRateProvider
import beer.thierry.centsible.api.services.integrations.ProviderRate
import java.time.LocalDate

class FrankfurterExchangeRateProvider(
    private val client: FrankfurterHttpClient,
) : IExchangeRateProvider {

    override val source: String = "frankfurter"

    override fun fetchRate(base: Currency, quote: Currency, date: LocalDate): ProviderRate =
        client.fetchRate(base, quote, date)
}
