package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.export.ExportTransactionRow
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.PdfRenderer
import beer.thierry.centsibleexport.render.RenderedExport
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private const val DEFAULT_LOCALE_TAG = "en-GB"
private const val DEFAULT_CURRENCY = "EUR"

private val SLUG_PATTERN = Regex("[^a-z0-9]+")

private val log = LoggerFactory.getLogger("beer.thierry.centsibleexport.render.impl.RenderingHelpers")

internal fun ExportRequest.locale(): Locale =
    meta.locale.takeIf { it.isNotBlank() }?.let(Locale::forLanguageTag) ?: Locale.forLanguageTag(DEFAULT_LOCALE_TAG)

/** Human-formatted "now" timestamp shared by [baseMeta] and JSON envelope headers. */
internal fun generatedAt(request: ExportRequest): String =
    OffsetDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", request.locale()))

internal fun baseMeta(request: ExportRequest): Map<String, Any?> =
    mapOf(
        "userName" to request.meta.userDisplayName.ifBlank { "Account holder" },
        "userEmail" to request.meta.userEmail,
        "currency" to request.meta.currency.ifBlank { DEFAULT_CURRENCY },
        "locale" to request.meta.locale.ifBlank { DEFAULT_LOCALE_TAG },
        "generatedAt" to generatedAt(request),
    )

/** Parsed transactions-export filters, shared by the PDF, CSV and JSON transactions renderers. */
internal data class TransactionFilters(
    val userId: UUID,
    val accountIds: List<UUID>,
    val fromDate: LocalDate?,
    val toDate: LocalDate?,
    val categoryIds: List<Long>,
)

/** Parses the transactions filter fields off the proto body. Guards that the body is present. */
internal fun ExportRequest.transactionFilters(): TransactionFilters {
    require(hasTransactions()) { "ExportRequest missing transactions body" }
    val body = transactions
    return TransactionFilters(
        userId = UUID.fromString(meta.userId),
        accountIds = body.accountIdsList.map(UUID::fromString),
        fromDate = body.fromDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse),
        toDate = body.toDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse),
        categoryIds = body.categoryIdsList.toList(),
    )
}

internal fun formatDate(date: LocalDate?, locale: Locale, pattern: String = "d MMM yyyy"): String =
    date?.format(DateTimeFormatter.ofPattern(pattern, locale)) ?: "—"

/** Stable, sortable epoch-second suffix for generated PDF filenames. */
internal fun filenameTimestamp(): Long = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()

internal fun slug(s: String): String =
    s.lowercase().replace(SLUG_PATTERN, "-").trim('-').ifBlank { "export" }

/**
 * Filename stem for a per-contact lendings export: `lendings-<slug(contactName)>`, so PDF, CSV and
 * JSON agree (CSV/JSON previously fell back to the raw contact UUID). Re-fetches the contact name
 * (cheap, and the contact is already known to exist by the time the filename is built); falls back
 * to the contact id if it can't be loaded.
 */
internal fun lendingsPerContactStem(data: IExportDataRepository, request: ExportRequest): String {
    val contactId = request.lendingsPerContact.contactId
    val name = runCatching {
        data.fetchContactSummary(UUID.fromString(request.meta.userId), UUID.fromString(contactId))?.contactName
    }.getOrNull()
    return "lendings-${slug(name ?: contactId)}"
}

/** The message takes no args: this app has no MessageSource, so the key reaches the UI bare and cannot interpolate them. */
internal fun IExportDataRepository.fetchTransactionsCapped(
    userId: UUID,
    accountIds: List<UUID>,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    categoryIds: List<Long>,
    maxRows: Int,
): List<ExportTransactionRow> {
    val rows = fetchTransactionsForExport(userId, accountIds, fromDate, toDate, categoryIds, maxRows + 1)
    if (rows.size > maxRows) {
        log.warn("Refusing oversized transactions export userId={} maxRows={}", userId, maxRows)
        throw LocalizedException.BadRequest("error.export.tooManyTransactions")
    }
    return rows
}

/**
 * Renders [template] with [context] and packages it as a [RenderedExport] whose filename is
 * `<filenameStem>-<filenameTimestamp>.pdf`.
 */
internal fun PdfRenderer.renderExport(
    template: String,
    filenameStem: String,
    context: Map<String, Any?>,
): RenderedExport = RenderedExport(
    bytes = renderHtmlToPdf(template, context),
    filename = "$filenameStem-${filenameTimestamp()}.pdf",
)
