package beer.thierry.centsible.imports.csv

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.CsvDialect
import beer.thierry.centsible.imports.core.FileFormatParser
import beer.thierry.centsible.imports.core.ParseHints
import beer.thierry.centsible.imports.core.ParseWarning
import beer.thierry.centsible.imports.core.ParsedFile
import beer.thierry.centsible.imports.core.importRow
import beer.thierry.centsible.imports.core.logWarningSummary
import beer.thierry.centsible.imports.core.requireDefaultCategoryId
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.StringReader
import java.io.UncheckedIOException
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val CURRENCY_NOISE = setOf('€', '$', '£', '¥', '₣', '¤', '₽', '₹')
private val WINDOWS_1252 = Charset.forName("windows-1252")

@Component
class CsvFileParser : FileFormatParser {
    private val log = LoggerFactory.getLogger(javaClass)

    override val id = "csv"
    override val displayName = "CSV"
    override val supportedMimeTypes = setOf("text/csv", "application/csv", "text/plain")
    override val supportedExtensions = setOf("csv", "tsv", "txt")
    override val requiresMapping = true

    /**
     * 1 KB is enough to see the header line of any real-world CSV without buffering the whole
     * file just to answer "is this CSV?". We accept any of comma/semicolon/tab as a delimiter so
     * European exports (semicolon, because their decimal separator is a comma) and spreadsheet
     * TSVs are recognized too.
     */
    override fun sniff(bytes: ByteArray, filename: String): Boolean {
        val sample = bytes.take(1024).toByteArray().toString(Charsets.UTF_8)
        if (sample.isBlank()) return false
        val firstLine = sample.lineSequence().firstOrNull { it.isNotBlank() } ?: return false
        return firstLine.contains(',') || firstLine.contains(';') || firstLine.contains('\t')
    }

    /**
     * CSV-specific inspection used by the REST layer. Detects the dialect (delimiter, encoding,
     * header presence) and returns the header plus up to [sampleRows] data rows.
     */
    fun probe(bytes: ByteArray, hints: ParseHints, sampleRows: Int = 5): CsvProbe {
        val dialect = hints.csvDialect ?: detectDialect(bytes)
        val all = readAllRecords(bytes, dialect).getOrElse { ex ->
            log.warn("CSV unparseable bytes={} reason={}", bytes.size, ex.message)
            return CsvProbe(
                header = emptyList(),
                sample = emptyList(),
                detectedDialect = dialect,
                warnings = listOf(ParseWarning("csv.unparseable", ex.message ?: "CSV could not be parsed", null)),
            )
        }
        val header = if (dialect.hasHeader && all.isNotEmpty()) all.first() else emptyList()
        val dataStart = if (dialect.hasHeader) 1 else 0
        val sample = all.drop(dataStart).take(sampleRows)
        return CsvProbe(header = header, sample = sample, detectedDialect = dialect)
    }

    override fun parse(bytes: ByteArray, hints: ParseHints): ParsedFile {
        log.debug("Parsing CSV file bytes={} encodingHint={}", bytes.size, hints.csvDialect?.encoding)
        try {
            val mapping = requireNotNull(hints.csvMapping) {
                "CsvFileParser requires ParseHints.csvMapping; call probe() and let the user confirm before parsing."
            }
            val defaultCategoryId = requireDefaultCategoryId(hints, "CsvFileParser")
            val dialect = hints.csvDialect ?: detectDialect(bytes)
            val all = readAllRecords(bytes, dialect).getOrElse { ex ->
                log.warn("CSV unparseable bytes={} reason={}", bytes.size, ex.message)
                return ParsedFile(
                    rows = emptyList(),
                    warnings = listOf(ParseWarning("csv.unparseable", ex.message ?: "CSV could not be parsed", null)),
                )
            }
            val dataStart = if (dialect.hasHeader) 1 else 0
            val dateParser = DateTimeFormatter.ofPattern(mapping.dateFormat, hints.locale ?: Locale.ENGLISH)

            val rows = mutableListOf<ImportTransactionRow>()
            val warnings = mutableListOf<ParseWarning>()

            all.drop(dataStart).forEachIndexed { index, cells ->
                val sourceRow = index + dataStart + 1
                val parsed = parseRow(cells, mapping, dateParser, defaultCategoryId, sourceRow, warnings)
                if (parsed != null) rows.add(parsed)
            }
            logWarningSummary("CSV", warnings, log)
            log.info("Parsed file format=csv rows={} warnings={}", rows.size, warnings.size)
            return ParsedFile(rows = rows, warnings = warnings)
        } catch (ex: Exception) {
            log.error("Failed to parse CSV file bytes={}", bytes.size, ex)
            throw ex
        }
    }

    /**
     * Run commons-csv over the whole file and materialize every record as a list of cells.
     * Centralised so [probe] and [parse] both use the same parsing pass and we don't need to
     * fight commons-csv's evolving header-handling API.
     *
     * Tokenizing is where a malformed file blows up — a download truncated inside a quoted field,
     * or a stray quote under a dialect whose quote char doesn't match, leaves commons-csv at EOF
     * with an unfinished token. That is a property of the upload, not a bug, so it comes back as a
     * failed [Result] for callers to report as a `csv.unparseable` warning. The commons-csv cause
     * carries the diagnosable message ("(startline 2) EOF reached before encapsulated token
     * finished"); the [UncheckedIOException] wrapping it only repeats that cause's toString.
     */
    private fun readAllRecords(bytes: ByteArray, dialect: CsvDialect): Result<List<List<String>>> {
        val text = decode(bytes, dialect.encoding)
        val format = csvFormat(dialect)
        return try {
            Result.success(
                CSVParser.parse(StringReader(text), format).use { parser ->
                    parser.map { record -> record.toList() }
                }
            )
        } catch (ex: UncheckedIOException) {
            Result.failure(ex.cause ?: ex)
        } catch (ex: IOException) {
            Result.failure(ex)
        } catch (ex: IllegalStateException) {
            Result.failure(ex)
        }
    }

    private fun parseRow(
        cells: List<String>,
        mapping: CsvColumnMapping,
        dateParser: DateTimeFormatter,
        defaultCategoryId: Long,
        sourceRow: Int,
        warnings: MutableList<ParseWarning>,
    ): ImportTransactionRow? {
        fun cell(index: Int?): String? = index?.takeIf { it in cells.indices }?.let { cells[it].trim() }

        val dateRaw = cell(mapping.dateColumn)
        val descRaw = cell(mapping.descriptionColumn) ?: ""

        val date = try {
            dateRaw?.let { LocalDate.parse(it, dateParser) }
        } catch (_: DateTimeParseException) {
            null
        }
        if (date == null) {
            warnings.add(ParseWarning("row.skipped.bad-date", "Unparseable date '$dateRaw'", sourceRow))
            return null
        }

        val amount = resolveAmount(
            mapping,
            cell(mapping.amountColumn),
            cell(mapping.debitColumn),
            cell(mapping.creditColumn),
        )
        if (amount == null || amount.signum() == 0) {
            warnings.add(ParseWarning("row.skipped.missing-amount", "Row missing or zero amount", sourceRow))
            return null
        }

        val counterparty = cell(mapping.counterpartyColumn)
        val description = descRaw.ifBlank { counterparty.orEmpty() }.ifBlank { "(no description)" }.take(255)
        val currency = Currency.parseOrNull(cell(mapping.currencyColumn))
        val categoryName = cell(mapping.categoryColumn)

        return importRow(amount, description, date, defaultCategoryId, currency, categoryName)
    }

    private fun resolveAmount(
        mapping: CsvColumnMapping,
        amountRaw: String?,
        debitRaw: String?,
        creditRaw: String?,
    ): BigDecimal? {
        val single = amountRaw?.let { parseDecimal(it, mapping) }
        if (single != null) return single

        val debit = debitRaw?.let { parseDecimal(it, mapping) }
        val credit = creditRaw?.let { parseDecimal(it, mapping) }
        return when {
            debit != null && debit.signum() != 0 -> if (mapping.debitsArePositive) debit.negate() else debit
            credit != null && credit.signum() != 0 -> credit
            else -> null
        }
    }

    private fun parseDecimal(raw: String, mapping: CsvColumnMapping): BigDecimal? {
        if (raw.isBlank()) return null
        val cleaned = buildString {
            for (ch in raw) {
                when {
                    ch.isDigit() || ch == '-' || ch == '+' -> append(ch)
                    ch == mapping.decimalSeparator -> append('.')
                    mapping.thousandsSeparator != null && ch == mapping.thousandsSeparator -> Unit
                    ch.isWhitespace() || ch in CURRENCY_NOISE -> Unit
                    else -> return null
                }
            }
        }
        return runCatching { BigDecimal(cleaned) }.getOrNull()
    }

    /**
     * A byte-order mark authoritatively identifies the encoding and must never leak into the
     * first header cell (it would break profile matching and the date column). When no BOM is
     * present the configured encoding applies — except that bytes which are not valid UTF-8
     * cannot honestly be decoded as such, so they fall back to windows-1252, the de-facto
     * encoding of the German/French bank exports this importer targets.
     */
    private fun decode(bytes: ByteArray, encoding: String): String {
        decodeByBom(bytes)?.let { return it }
        val requested = runCatching { Charset.forName(encoding) }.getOrDefault(Charsets.UTF_8)
        if (requested == Charsets.UTF_8 && !isValidUtf8(bytes)) {
            log.info("CSV bytes are not valid UTF-8; decoding as windows-1252 instead")
            return String(bytes, WINDOWS_1252)
        }
        return String(bytes, requested)
    }

    /**
     * The magic-byte inspection shared by [decodeByBom] and [detectEncoding]: returns the charset a
     * byte-order mark authoritatively identifies together with the BOM's byte length (so callers can
     * strip it), or null when no BOM is present. Keeping the byte checks in one place stops the three
     * decode paths from drifting apart.
     */
    private fun detectBomCharset(bytes: ByteArray): Pair<Charset, Int>? = when {
        bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
            Charsets.UTF_8 to 3
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
            Charsets.UTF_16LE to 2
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
            Charsets.UTF_16BE to 2
        else -> null
    }

    private fun decodeByBom(bytes: ByteArray): String? =
        detectBomCharset(bytes)?.let { (charset, prefix) -> String(bytes, prefix, bytes.size - prefix, charset) }

    private fun isValidUtf8(bytes: ByteArray): Boolean = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
    }.isSuccess

    private fun detectEncoding(bytes: ByteArray): String {
        detectBomCharset(bytes)?.let { return it.first.name() }
        return if (isValidUtf8(bytes)) "UTF-8" else "windows-1252"
    }

    /**
     * Heuristic: header-line delimiter counts decide. Semicolon wins when it outnumbers comma
     * (German/French bank exports — comma is the decimal separator there, so they use ';').
     * Tab wins only when present and there are no commas at all (spreadsheet TSV without comma
     * fields). Comma is the safe default for everything else. The detected encoding rides along
     * so probe/parse and the mapping wizard all decode the file the same way.
     */
    private fun detectDialect(bytes: ByteArray): CsvDialect {
        val encoding = detectEncoding(bytes)
        val sample = decode(bytes, encoding)
        val firstLine = sample.lineSequence().firstOrNull { it.isNotBlank() }
            ?: return CsvDialect(encoding = encoding)
        val delimiter = when {
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            firstLine.count { it == '\t' } > 0 && firstLine.count { it == ',' } == 0 -> '\t'
            else -> ','
        }
        return CsvDialect(delimiter = delimiter, encoding = encoding)
    }

    /**
     * Build a [CSVFormat] for the given dialect. Uses the legacy `withX` API which is stable
     * across recent commons-csv versions; the newer Builder API churned its method signatures
     * (setHeader, build vs get) too often to be worth the deprecation warnings.
     */
    @Suppress("DEPRECATION")
    private fun csvFormat(dialect: CsvDialect): CSVFormat = CSVFormat.DEFAULT
        .withDelimiter(dialect.delimiter)
        .withQuote(dialect.quote)
        .withIgnoreEmptyLines(true)
        .withTrim()
}
