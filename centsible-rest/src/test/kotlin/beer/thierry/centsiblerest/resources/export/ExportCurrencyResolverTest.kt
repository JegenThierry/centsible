package beer.thierry.centsiblerest.resources.export

import beer.thierry.centsible.api.model.budgetaccount.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExportCurrencyResolverTest {

    @Test
    fun `picks the most common account currency`() {
        assertEquals("CHF", primaryCurrencyOf(listOf(Currency.CHF, Currency.CHF, Currency.EUR)))
    }

    @Test
    fun `breaks ties by enum order so the result is independent of input order`() {
        assertEquals("EUR", primaryCurrencyOf(listOf(Currency.CHF, Currency.EUR)))
        assertEquals("EUR", primaryCurrencyOf(listOf(Currency.EUR, Currency.CHF)))
    }

    @Test
    fun `falls back to EUR when the user has no accounts`() {
        assertEquals("EUR", primaryCurrencyOf(emptyList()))
    }
}
