package beer.thierry.centsible.imports.csv

import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.CsvDialect
import beer.thierry.centsible.imports.core.FileFormatParser
import beer.thierry.centsible.imports.core.ParseHints
import beer.thierry.centsible.imports.core.ParseWarning
import beer.thierry.centsible.imports.core.ParsedFile
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.stereotype.Component
import java.io.StringReader
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * CSV file parser. Stateless; one Spring bean serves every import. The CSV format itself can't
 * tell the parser what each column means, so [requiresMapping] is true and [parse] needs
 * [ParseHints.csvMapping]. Use [probe] from the REST layer to extract the header for profile
 * matching before asking the user to confirm or tweak the mapping.
 */
@Component
class CsvFileParser : FileFormatParser {
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
        val all = readAllRecords(bytes, dialect)
        val header = if (dialect.hasHeader && all.isNotEmpty()) all.first() else emptyList()
        val dataStart = if (dialect.hasHeader) 1 else 0
        val sample = all.drop(dataStart).take(sampleRows)
        return CsvProbe(header = header, sample = sample, detectedDialect = dialect)
    }

    override fun parse(bytes: ByteArray, hints: ParseHints): ParsedFile {
        val mapping = requireNotNull(hints.csvMapping) {
            "CsvFileParser requires ParseHints.csvMapping; call probe() and let the user confirm before parsing."
        }
        val defaultCategoryId = requireNotNull(hints.defaultCategoryId) {
            "CsvFileParser requires ParseHints.defaultCategoryId so unmapped rows still satisfy validation."
        }
        val dialect = hints.csvDialect ?: detectDialect(bytes)
        val all = readAllRecords(bytes, dialect)
        val dataStart = if (dialect.hasHeader) 1 else 0
        val dateParser = DateTimeFormatter.ofPattern(mapping.dateFormat, hints.locale ?: Locale.ENGLISH)

        val rows = mutableListOf<ImportTransactionRow>()
        val warnings = mutableListOf<ParseWarning>()

        all.drop(dataStart).forEachIndexed { index, cells ->
            val sourceRow = index + dataStart + 1
            val parsed = parseRow(cells, mapping, dateParser, defaultCategoryId, sourceRow, warnings)
            if (parsed != null) rows.add(parsed)
        }
        return ParsedFile(rows = rows, warnings = warnings)
    }

    /**
     * Run commons-csv over the whole file and materialize every record as a list of cells.
     * Centralised so [probe] and [parse] both use the same parsing pass and we don't need to
     * fight commons-csv's evolving header-handling API.
     */
    private fun readAllRecords(bytes: ByteArray, dialect: CsvDialect): List<List<String>> {
        val text = decode(bytes, dialect.encoding)
        val format = csvFormat(dialect)
        return CSVParser.parse(StringReader(text), format).use { parser ->
            parser.map { record -> (0 until record.size()).map { i -> record[i] ?: "" } }
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

        val description = descRaw.ifBlank { "(no description)" }.take(255)

        return ImportTransactionRow(
            amount = amount.abs(),
            categoryId = defaultCategoryId,
            description = description,
            transactionDate = date,
            type = null,
        )
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
                if (ch.isDigit() || ch == '-' || ch == '+') append(ch)
                else if (ch == mapping.decimalSeparator) append('.')
                // Drop currency symbols, thousands separators, whitespace.
            }
        }
        return runCatching { BigDecimal(cleaned) }.getOrNull()
    }

    private fun decode(bytes: ByteArray, encoding: String): String =
        bytes.toString(runCatching { Charset.forName(encoding) }.getOrDefault(Charsets.UTF_8))

    /**
     * Heuristic: header-line delimiter counts decide. Semicolon wins when it outnumbers comma
     * (German/French bank exports — comma is the decimal separator there, so they use ';').
     * Tab wins only when present and there are no commas at all (spreadsheet TSV without comma
     * fields). Comma is the safe default for everything else.
     */
    private fun detectDialect(bytes: ByteArray): CsvDialect {
        val sample = bytes.take(4096).toByteArray().toString(Charsets.UTF_8)
        val firstLine = sample.lineSequence().firstOrNull { it.isNotBlank() } ?: return CsvDialect()
        val delimiter = when {
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            firstLine.count { it == '\t' } > 0 && firstLine.count { it == ',' } == 0 -> '\t'
            else -> ','
        }
        return CsvDialect(delimiter = delimiter)
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
