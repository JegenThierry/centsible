package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.repository.CachedRate
import beer.thierry.centsible.api.repository.IExchangeRateRepository
import beer.thierry.centsible.core.services.currency.CurrencyConversionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate

class CurrencyConversionServiceTest {

    private val repo = mock(IExchangeRateRepository::class.java)
    private val date: LocalDate = LocalDate.of(2024, 1, 2)

    private fun service(staleDays: Long = 7): CurrencyConversionService =
        CurrencyConversionService(repo, staleDays)

    @Test
    fun `same-currency conversion is a no-op and touches neither cache nor provider`() {
        val result = service().convert(BigDecimal("100.00"), Currency.EUR, Currency.EUR, date)

        assertTrue(result.sameCurrency)
        assertEquals(BigDecimal("100.00"), result.convertedAmount)
        assertEquals(BigDecimal.ONE, result.rate)
        verifyNoInteractions(repo)
    }

    @Test
    fun `cache hit converts without calling the provider`() {
        `when`(repo.findRate(Currency.USD, Currency.EUR, date))
            .thenReturn(CachedRate(BigDecimal("0.90"), date, "frankfurter"))

        val result = service().convert(BigDecimal("10.00"), Currency.USD, Currency.EUR, date)

        assertEquals(BigDecimal("9.00"), result.convertedAmount)
        assertFalse(result.sameCurrency)
    }

    @Test
    fun `cache miss with no rate throws a localized error`() {
        `when`(repo.findRate(Currency.USD, Currency.EUR, date)).thenReturn(null)
        `when`(repo.findLatestOnOrBefore(Currency.USD, Currency.EUR, date)).thenReturn(null)

        assertThrows(LocalizedException.BadRequest::class.java) {
            service().convert(BigDecimal("10.00"), Currency.USD, Currency.EUR, date)
        }
    }

    @Test
    fun `conversion rounds half-even to two decimals`() {
        `when`(repo.findRate(Currency.USD, Currency.EUR, date)).thenReturn(CachedRate(BigDecimal("2.345"), date, "x"))

        val result = service().convert(BigDecimal("1.00"), Currency.USD, Currency.EUR, date)

        assertEquals(BigDecimal("2.34"), result.convertedAmount)
    }

    @Test
    fun `uses a recent cached rate within tolerance when no exact rate exists`() {
        `when`(repo.findRate(Currency.USD, Currency.EUR, date)).thenReturn(null)
        `when`(repo.findLatestOnOrBefore(Currency.USD, Currency.EUR, date))
            .thenReturn(CachedRate(BigDecimal("0.80"), date.minusDays(3), "frankfurter"))

        val result = service(staleDays = 7).convert(BigDecimal("10.00"), Currency.USD, Currency.EUR, date)

        assertEquals(BigDecimal("8.00"), result.convertedAmount)
        assertEquals(date.minusDays(3), result.rateDate)
    }

    @Test
    fun `throws when only a too-stale cached rate exists`() {
        `when`(repo.findRate(Currency.USD, Currency.EUR, date)).thenReturn(null)
        `when`(repo.findLatestOnOrBefore(Currency.USD, Currency.EUR, date))
            .thenReturn(CachedRate(BigDecimal("0.80"), date.minusDays(30), "frankfurter"))

        assertThrows(LocalizedException.BadRequest::class.java) {
            service(staleDays = 7).convert(BigDecimal("10.00"), Currency.USD, Currency.EUR, date)
        }
    }

    @Test
    fun `a converted amount beyond DECIMAL(15,2) capacity is rejected`() {
        `when`(repo.findRate(Currency.USD, Currency.EUR, date)).thenReturn(CachedRate(BigDecimal("1000"), date, "x"))

        assertThrows(LocalizedException.BadRequest::class.java) {
            service().convert(BigDecimal("100000000000.00"), Currency.USD, Currency.EUR, date)
        }
    }
}