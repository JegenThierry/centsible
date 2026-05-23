package beer.thierry.centsiblerest.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

class YearMonthConverterTest {

    private val converter = YearMonthConverter()

    @Test
    fun `parses ISO YYYY-MM`() {
        assertEquals(YearMonth.of(2026, 5), converter.convert("2026-05"))
    }

    @Test
    fun `rejects malformed input with a descriptive IllegalArgumentException`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            converter.convert("not-a-year-month")
        }
        // The handler turns this into HTTP 400, so the message must be human-readable.
        assertTrue(ex.message!!.contains("YearMonth"))
        assertTrue(ex.message!!.contains("not-a-year-month"))
    }

    @Test
    fun `rejects swapped order MM-YYYY`() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.convert("05-2026")
        }
    }

    @Test
    fun `rejects month out of range`() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.convert("2026-13")
        }
    }
}
