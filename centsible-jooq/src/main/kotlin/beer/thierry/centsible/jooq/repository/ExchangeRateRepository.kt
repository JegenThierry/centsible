package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.repository.CachedRate
import beer.thierry.centsible.api.repository.IExchangeRateRepository
import beer.thierry.jooq.generated.tables.references.EXCHANGE_RATES
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class ExchangeRateRepository(private val dsl: DSLContext) : IExchangeRateRepository {

    override fun findRate(base: Currency, quote: Currency, rateDate: LocalDate): CachedRate? =
        dsl.select(EXCHANGE_RATES.RATE, EXCHANGE_RATES.RATE_DATE, EXCHANGE_RATES.SOURCE)
            .from(EXCHANGE_RATES)
            .where(
                EXCHANGE_RATES.BASE_CURRENCY.eq(base.name)
                    .and(EXCHANGE_RATES.QUOTE_CURRENCY.eq(quote.name))
                    .and(EXCHANGE_RATES.RATE_DATE.eq(rateDate))
            )
            .fetchOne { mapCachedRate(it) }

    override fun findLatestOnOrBefore(base: Currency, quote: Currency, onOrBefore: LocalDate): CachedRate? =
        dsl.select(EXCHANGE_RATES.RATE, EXCHANGE_RATES.RATE_DATE, EXCHANGE_RATES.SOURCE)
            .from(EXCHANGE_RATES)
            .where(
                EXCHANGE_RATES.BASE_CURRENCY.eq(base.name)
                    .and(EXCHANGE_RATES.QUOTE_CURRENCY.eq(quote.name))
                    .and(EXCHANGE_RATES.RATE_DATE.le(onOrBefore))
            )
            .orderBy(EXCHANGE_RATES.RATE_DATE.desc())
            .limit(1)
            .fetchOne { mapCachedRate(it) }

    override fun upsert(base: Currency, quote: Currency, rate: BigDecimal, rateDate: LocalDate, source: String) {
        dsl.insertInto(
            EXCHANGE_RATES,
            EXCHANGE_RATES.BASE_CURRENCY, EXCHANGE_RATES.QUOTE_CURRENCY,
            EXCHANGE_RATES.RATE, EXCHANGE_RATES.RATE_DATE, EXCHANGE_RATES.SOURCE,
        )
            .values(base.name, quote.name, rate, rateDate, source)
            .onConflict(EXCHANGE_RATES.BASE_CURRENCY, EXCHANGE_RATES.QUOTE_CURRENCY, EXCHANGE_RATES.RATE_DATE)
            .doNothing()
            .execute()
    }

    private fun mapCachedRate(record: Record): CachedRate = CachedRate(
        rate = record[EXCHANGE_RATES.RATE]!!,
        rateDate = record[EXCHANGE_RATES.RATE_DATE]!!,
        source = record[EXCHANGE_RATES.SOURCE]!!,
    )
}
