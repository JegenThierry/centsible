package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.budgetaccount.Currency
import java.math.BigDecimal
import java.time.LocalDate

interface IExchangeRateRepository {
    fun findRate(base: Currency, quote: Currency, rateDate: LocalDate): CachedRate?

    fun findLatestOnOrBefore(base: Currency, quote: Currency, onOrBefore: LocalDate): CachedRate?

    fun upsert(base: Currency, quote: Currency, rate: BigDecimal, rateDate: LocalDate, source: String)
}

data class CachedRate(
    val rate: BigDecimal,
    val rateDate: LocalDate,
    val source: String,
)
