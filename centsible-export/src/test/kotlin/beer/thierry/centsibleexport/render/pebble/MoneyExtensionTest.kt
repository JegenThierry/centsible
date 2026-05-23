package beer.thierry.centsibleexport.render.pebble

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyFilterTest {

    private val filter = MoneyFilter()

    private fun apply(input: Any?, currency: String? = null, locale: String? = null): Any {
        val args = mutableMapOf<String, Any?>()
        if (currency != null) args["currency"] = currency
        if (locale != null) args["locale"] = locale
        return filter.apply(input, args, null, null, 1)
    }

    @Test
    fun `null input renders as empty string`() {
        assertEquals("", apply(null))
    }

    @Test
    fun `BigDecimal renders with two fraction digits using the requested currency and locale`() {
        val out = apply(BigDecimal("1234.5"), currency = "EUR", locale = "en-GB") as String
        assertTrue(out.contains("1,234.50"))
        // Symbol for EUR in en-GB is €.
        assertTrue(out.contains("€"))
    }

    @Test
    fun `Number input is converted to BigDecimal preserving precision`() {
        val out = apply(99.9, currency = "USD", locale = "en-US") as String
        assertTrue(out.contains("99.90"))
        assertTrue(out.contains("$"))
    }

    @Test
    fun `String input that parses as a BigDecimal is formatted`() {
        val out = apply("7.5", currency = "EUR", locale = "en-GB") as String
        assertTrue(out.contains("7.50"))
    }

    @Test
    fun `String input that does not parse is returned untouched`() {
        assertEquals("not-a-number", apply("not-a-number"))
    }

    @Test
    fun `unsupported input type falls back to toString`() {
        // Objects without a numeric representation should not blow up the renderer.
        val custom = object {
            override fun toString(): String = "custom-rep"
        }
        assertEquals("custom-rep", apply(custom))
    }

    @Test
    fun `currency and locale default to EUR and en-GB when not supplied`() {
        val out = apply(BigDecimal("1.00")) as String
        assertTrue(out.contains("1.00"))
        assertTrue(out.contains("€"))
    }

    @Test
    fun `argument names are currency and locale`() {
        assertEquals(listOf("currency", "locale"), filter.argumentNames)
    }
}
