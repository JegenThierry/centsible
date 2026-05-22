package beer.thierry.centsible.imports.ofx

import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.imports.core.FileFormatParser
import beer.thierry.centsible.imports.core.ParseHints
import beer.thierry.centsible.imports.core.ParseWarning
import beer.thierry.centsible.imports.core.ParsedFile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Pragmatic OFX 1.x (SGML) and 2.x (XML) parser. Extracts transactions out of every STMTTRN
 * block in the file; ignores aggregates we don't need (balance, signons, status).
 *
 * Why not ofx4j? OFX statement extraction is simple enough to hand-roll, and a 60-line regex
 * parser sidesteps adding a transitive dependency tree (and dealing with ofx4j's date/amount
 * conversion quirks). If we ever need full OFX semantics (transfers, investment positions,
 * 401k details) we'd swap to ofx4j here without touching the SPI.
 */
@Component
class OfxFileParser : FileFormatParser {
    override val id = "ofx"
    override val displayName = "OFX / QFX"
    override val supportedMimeTypes = setOf(
        "application/x-ofx",
        "application/vnd.intu.qfx",
        "application/vnd.intu.qbo",
        "text/x-ofx",
    )
    override val supportedExtensions = setOf("ofx", "qfx", "qbo")
    override val requiresMapping = false

    override fun sniff(bytes: ByteArray, filename: String): Boolean {
        val head = bytes.take(1024).toByteArray().toString(Charsets.UTF_8).uppercase()
        return "OFXHEADER" in head || "<OFX>" in head
    }

    override fun parse(bytes: ByteArray, hints: ParseHints): ParsedFile {
        val text = stripPreamble(bytes.toString(Charsets.UTF_8))
        val defaultCategoryId = requireNotNull(hints.defaultCategoryId) {
            "OfxFileParser requires ParseHints.defaultCategoryId so unmapped rows still satisfy validation."
        }
        val detectedCurrency = TAG_PATTERN.find(text, key = "CURDEF")?.uppercase()

        val rows = mutableListOf<ImportTransactionRow>()
        val warnings = mutableListOf<ParseWarning>()

        STMTTRN_PATTERN.findAll(text).forEachIndexed { index, match ->
            val body = match.groupValues[1]
            val sourceRow = index + 1

            val amountRaw = TAG_PATTERN.find(body, key = "TRNAMT")
            val datePostedRaw = TAG_PATTERN.find(body, key = "DTPOSTED")
            val name = TAG_PATTERN.find(body, key = "NAME")
            val memo = TAG_PATTERN.find(body, key = "MEMO")
            val payee = TAG_PATTERN.find(body, key = "PAYEE")

            val amount = amountRaw?.let { runCatching { BigDecimal(it) }.getOrNull() }
            val date = datePostedRaw?.let(::parseOfxDate)

            if (amount == null || amount.signum() == 0) {
                warnings.add(ParseWarning("ofx.skipped.missing-amount", "STMTTRN missing or zero amount", sourceRow))
                return@forEachIndexed
            }
            if (date == null) {
                warnings.add(ParseWarning("ofx.skipped.bad-date", "Unparseable DTPOSTED '$datePostedRaw'", sourceRow))
                return@forEachIndexed
            }

            val description = listOfNotNull(name, payee, memo).firstOrNull { it.isNotBlank() }?.take(255)
                ?: "(no description)"

            rows.add(
                ImportTransactionRow(
                    amount = amount.abs(),
                    categoryId = defaultCategoryId,
                    description = description,
                    transactionDate = date,
                    type = null,
                )
            )
        }

        return ParsedFile(rows = rows, warnings = warnings, detectedCurrency = detectedCurrency)
    }

    /** Drop the SGML/HTTP-style headers (everything before the first `<`). */
    private fun stripPreamble(text: String): String {
        val firstAngle = text.indexOf('<')
        return if (firstAngle <= 0) text else text.substring(firstAngle)
    }

    /**
     * Parse the OFX date formats:
     *   - YYYYMMDD
     *   - YYYYMMDDHHMMSS
     *   - YYYYMMDDHHMMSS.XXX[-tz:TZNAME]
     * Only the date part matters for our row; the time and timezone are dropped.
     */
    private fun parseOfxDate(raw: String): LocalDate? {
        if (raw.length < 8) return null
        val datePart = raw.substring(0, 8)
        return runCatching {
            LocalDate.of(
                datePart.substring(0, 4).toInt(),
                datePart.substring(4, 6).toInt(),
                datePart.substring(6, 8).toInt(),
            )
        }.getOrNull()
    }

    private companion object {
        val STMTTRN_PATTERN = Regex("""<STMTTRN>([\s\S]*?)</STMTTRN>""", RegexOption.IGNORE_CASE)

        // Matches both OFX 1.x (no closing tag, value runs until next tag or EOL) and OFX 2.x
        // (closing tag). Capture group 1 is the tag value, trimmed downstream.
        val TAG_PATTERN = Regex("""<(\w+)>\s*([^<\n\r]*)""")

        fun Regex.find(input: CharSequence, key: String): String? {
            for (m in findAll(input)) {
                if (m.groupValues[1].equals(key, ignoreCase = true)) {
                    return m.groupValues[2].trim().takeIf { it.isNotBlank() }
                }
            }
            return null
        }
    }
}
