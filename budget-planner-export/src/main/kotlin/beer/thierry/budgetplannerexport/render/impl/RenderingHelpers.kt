package beer.thierry.budgetplannerexport.render.impl

import beer.thierry.budgetplanner.export.proto.ExportRequest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DEFAULT_LOCALE_TAG = "en-GB"
private const val DEFAULT_CURRENCY = "EUR"

internal fun ExportRequest.locale(): Locale =
    meta.locale.takeIf { it.isNotBlank() }?.let(Locale::forLanguageTag) ?: Locale.forLanguageTag(DEFAULT_LOCALE_TAG)

internal fun baseMeta(request: ExportRequest): Map<String, Any?> {
    val locale = request.locale()
    val timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale))
    return mapOf(
        "userName" to request.meta.userDisplayName.ifBlank { "Account holder" },
        "userEmail" to request.meta.userEmail,
        "currency" to request.meta.currency.ifBlank { DEFAULT_CURRENCY },
        "locale" to request.meta.locale.ifBlank { DEFAULT_LOCALE_TAG },
        "generatedAt" to timestamp,
    )
}

internal fun formatDate(date: LocalDate?, locale: Locale, pattern: String = "d MMM yyyy"): String =
    date?.format(DateTimeFormatter.ofPattern(pattern, locale)) ?: "—"
