package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.PdfRenderer
import beer.thierry.centsibleexport.render.RenderedExport
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
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

/** Stable, sortable epoch-second suffix for generated PDF filenames. */
internal fun filenameTimestamp(): Long = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()

internal fun slug(s: String): String =
    s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "export" }

/**
 * Renders [template] with [context] and packages it as a [RenderedExport] whose filename is
 * `<filenameStem>-<filenameTimestamp>.pdf`.
 */
internal fun PdfRenderer.renderExport(
    template: String,
    filenameStem: String,
    context: Map<String, Any?>,
): RenderedExport = RenderedExport(
    pdf = renderHtmlToPdf(template, context),
    filename = "$filenameStem-${filenameTimestamp()}.pdf",
)
