package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.budgetaccount.Currency
import java.math.BigDecimal
import java.time.LocalDate

interface IExchangeRateRepository {
    fun findRate(base: Currency, quote: Currency, rateDate: LocalDate): CachedRate?

    /** Most recent cached rate dated on or before [onOrBefore] (carry-forward fallback for non-trading days), or null. */
    fun findLatestOnOrBefore(base: Currency, quote: Currency, onOrBefore: LocalDate): CachedRate?

    /** Caches a rate for the (base, quote, date) key; existing rows are kept (first write wins, no overwrite). */
    fun upsert(base: Currency, quote: Currency, rate: BigDecimal, rateDate: LocalDate, source: String)
}

data class CachedRate(
    val rate: BigDecimal,
    val rateDate: LocalDate,
    val source: String,
)
