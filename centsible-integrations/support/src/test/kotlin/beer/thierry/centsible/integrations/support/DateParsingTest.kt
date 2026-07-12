package beer.thierry.centsible.integrations.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DateParsingTest {

    @Test
    fun `parses a date-only string as midnight at UTC`() {
        assertEquals(
            OffsetDateTime.of(2026, 6, 8, 0, 0, 0, 0, ZoneOffset.UTC),
            parseDateOnlyAtUtc("2026-06-08"),
        )
    }

    @Test
    fun `returns null for a non-date string`() {
        assertNull(parseDateOnlyAtUtc("2026-06-08T12:30:00Z"))
        assertNull(parseDateOnlyAtUtc("not-a-date"))
        assertNull(parseDateOnlyAtUtc(""))
    }
}
