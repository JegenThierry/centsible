package beer.thierry.centsible.integrations.support

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Parses an ISO date-only string (`yyyy-MM-dd`) as midnight at UTC, returning null if it is not a
 * valid date. Providers use this as the fallback when an upstream gives a date with no time/offset
 * (PayPal's date-only `transaction_initiation_date`, GoCardless's `bookingDate`/`valueDate`), so
 * the "no time component → assume start of day in UTC" convention lives in one place.
 */
fun parseDateOnlyAtUtc(value: String): OffsetDateTime? =
    try {
        LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.UTC)
    } catch (_: DateTimeParseException) {
        null
    }
