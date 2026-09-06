package beer.thierry.centsible.core.services.currency

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.currency.ConversionRequest
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.repository.IExchangeRateRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class CurrencyConversionService(
    private val exchangeRateRepository: IExchangeRateRepository,
    @param:Value("\${fx.stale-tolerance-days:7}") private val staleToleranceDays: Long,
) : ICurrencyConversionService {

    private val log = LoggerFactory.getLogger(CurrencyConversionService::class.java)

    override fun convert(amount: BigDecimal, from: Currency, to: Currency, date: LocalDate): ConversionResult {
        if (from == to) return sameCurrency(amount, to, date)
        return applyRate(amount, from, to, resolveRate(from, to, date))
    }

    override fun convertAll(requests: List<ConversionRequest>, to: Currency): List<ConversionResult> {
        val rates = HashMap<Pair<Currency, LocalDate>, ResolvedRate>()
        val today = LocalDate.now()
        return requests.map { request ->
            if (request.from == to) return@map sameCurrency(request.amount, to, request.date)
            val rate = rates.getOrPut(request.from to minOf(request.date, today)) {
                resolveRate(request.from, to, request.date)
            }
            applyRate(request.amount, request.from, to, rate)
        }
    }

    private fun resolveRate(from: Currency, to: Currency, date: LocalDate): ResolvedRate {
        val lookupDate = minOf(date, LocalDate.now())
        val cached = exchangeRateRepository.findRate(from, to, lookupDate)
        return cached?.let { ResolvedRate(it.rate, it.rateDate) } ?: fetchAndCache(from, to, lookupDate)
    }

    private fun applyRate(amount: BigDecimal, from: Currency, to: Currency, resolved: ResolvedRate): ConversionResult {
        val converted = amount.multiply(resolved.rate).setScale(SCALE, RoundingMode.HALF_EVEN)
        if (converted.abs() > MAX_CONVERTED_AMOUNT) {
            throw LocalizedException.BadRequest("error.fx.convertedAmountTooLarge")
        }
        return ConversionResult(
            convertedAmount = converted,
            originalAmount = amount.setScale(SCALE, RoundingMode.HALF_EVEN),
            originalCurrency = from,
            accountCurrency = to,
            rate = resolved.rate,
            rateDate = resolved.rateDate,
            sameCurrency = false,
        )
    }

    private fun sameCurrency(amount: BigDecimal, currency: Currency, date: LocalDate): ConversionResult {
        val scaled = amount.setScale(SCALE, RoundingMode.HALF_EVEN)
        return ConversionResult(
            convertedAmount = scaled,
            originalAmount = scaled,
            originalCurrency = currency,
            accountCurrency = currency,
            rate = BigDecimal.ONE,
            rateDate = date,
            sameCurrency = true,
        )
    }

    private fun fetchAndCache(from: Currency, to: Currency, date: LocalDate): ResolvedRate {
        val fallback = exchangeRateRepository.findLatestOnOrBefore(from, to, date)
        if (fallback != null && !fallback.rateDate.isBefore(date.minusDays(staleToleranceDays))) {
            log.warn("Using stale cached FX rate {}->{} from {} for {}", from, to, fallback.rateDate, date)
            return ResolvedRate(fallback.rate, fallback.rateDate)
        }

        throw LocalizedException.BadRequest("error.fx.rateUnavailable", from.name, to.name, date.toString())
    }

    private data class ResolvedRate(val rate: BigDecimal, val rateDate: LocalDate)

    private companion object {
        const val SCALE = 2
        val MAX_CONVERTED_AMOUNT: BigDecimal = BigDecimal("9999999999999.99")
    }
}
