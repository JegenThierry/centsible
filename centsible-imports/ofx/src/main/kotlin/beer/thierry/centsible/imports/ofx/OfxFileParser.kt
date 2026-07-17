package beer.thierry.centsible.imports.ofx

import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.imports.core.FileFormatParser
import beer.thierry.centsible.imports.core.ParseHints
import beer.thierry.centsible.imports.core.ParseWarning
import beer.thierry.centsible.imports.core.ParsedFile
import beer.thierry.centsible.imports.core.importRow
import beer.thierry.centsible.imports.core.logWarningSummary
import beer.thierry.centsible.imports.core.requireDefaultCategoryId
import com.webcohesion.ofx4j.OFXException
import com.webcohesion.ofx4j.domain.data.ResponseEnvelope
import com.webcohesion.ofx4j.domain.data.banking.BankingResponseMessageSet
import com.webcohesion.ofx4j.domain.data.common.StatementResponse
import com.webcohesion.ofx4j.domain.data.common.Transaction
import com.webcohesion.ofx4j.domain.data.creditcard.CreditCardResponseMessageSet
import com.webcohesion.ofx4j.io.AggregateUnmarshaller
import com.webcohesion.ofx4j.io.OFXParseException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.time.ZoneOffset

/**
 * Parses OFX 1.x (SGML) and 2.x (XML) bank- and credit-card statement files via ofx4j. The
 * library handles preamble stripping, SGML-vs-XML auto-detection, tag escaping, and date/amount
 * conversion; this parser only translates the OFX domain into [ImportTransactionRow].
 *
 * Times are normalised to UTC: OFX DTPOSTED is bank-local with an explicit offset, but we only
 * carry the calendar day forward, so collapsing to UTC keeps imports deterministic across
 * timezones rather than depending on the JVM default.
 */
@Component
class OfxFileParser : FileFormatParser {
    private val log = LoggerFactory.getLogger(javaClass)

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
        log.debug("Parsing OFX file bytes={}", bytes.size)
        try {
            val defaultCategoryId = requireDefaultCategoryId(hints, "OfxFileParser")

            val envelope = try {
                AggregateUnmarshaller(ResponseEnvelope::class.java).unmarshal(ByteArrayInputStream(bytes))
            } catch (ex: OFXParseException) {
                log.warn("OFX unparseable bytes={} reason={}", bytes.size, ex.message)
                return ParsedFile(
                    rows = emptyList(),
                    warnings = listOf(ParseWarning("ofx.unparseable", ex.message ?: "OFX could not be parsed", null)),
                )
            } catch (ex: OFXException) {
                log.warn("OFX unparseable bytes={} reason={}", bytes.size, ex.message)
                return ParsedFile(
                    rows = emptyList(),
                    warnings = listOf(ParseWarning("ofx.unparseable", ex.message ?: "OFX could not be parsed", null)),
                )
            }

            val rows = mutableListOf<ImportTransactionRow>()
            val warnings = mutableListOf<ParseWarning>()
            var detectedCurrency: String? = null
            var sourceRow = 0

            for (set in envelope.messageSets) {
                val statementResponses: List<StatementBlock> = when (set) {
                    is BankingResponseMessageSet -> statementBlocks(set.statementResponses.orEmpty()) { it.message }
                    is CreditCardResponseMessageSet -> statementBlocks(set.statementResponses.orEmpty()) { it.message }
                    else -> continue
                }
                for (block in statementResponses) {
                    if (detectedCurrency == null) detectedCurrency = block.currencyCode
                    for (tx in block.transactions) {
                        sourceRow += 1
                        val row = mapTransaction(tx, defaultCategoryId, sourceRow, warnings)
                        if (row != null) rows.add(row)
                    }
                }
            }

            logWarningSummary("OFX", warnings, log)
            log.info("Parsed file format=ofx rows={} warnings={}", rows.size, warnings.size)
            return ParsedFile(rows = rows, warnings = warnings, detectedCurrency = detectedCurrency?.uppercase())
        } catch (ex: Exception) {
            log.error("Failed to parse OFX file bytes={}", bytes.size, ex)
            throw ex
        }
    }

    private fun mapTransaction(
        tx: Transaction,
        defaultCategoryId: Long,
        sourceRow: Int,
        warnings: MutableList<ParseWarning>,
    ): ImportTransactionRow? {
        val amount = tx.bigDecimalAmount?.takeIf { it.signum() != 0 }
        if (amount == null) {
            warnings.add(ParseWarning("ofx.skipped.missing-amount", "STMTTRN missing or zero amount", sourceRow))
            return null
        }
        val date = tx.datePosted?.toInstant()?.atZone(ZoneOffset.UTC)?.toLocalDate()
        if (date == null) {
            warnings.add(ParseWarning("ofx.skipped.bad-date", "Missing or unparseable DTPOSTED", sourceRow))
            return null
        }

        val name = tx.name?.takeIf { it.isNotBlank() }
        val memo = tx.memo?.takeIf { it.isNotBlank() }
        val payee = tx.payee?.name?.takeIf { it.isNotBlank() }
        val description = listOfNotNull(name, payee, memo).firstOrNull()?.take(255) ?: "(no description)"

        return importRow(amount, description, date, defaultCategoryId)
    }

    /**
     * Shared tail for the banking and credit-card arms of [parse]: unwrap each response's statement
     * [message] (the two ofx4j wrapper types share no supertype, so the caller supplies the accessor)
     * and project it onto a [StatementBlock]. Both statement messages extend [StatementResponse], so
     * currency and transaction extraction are identical once the message is in hand.
     */
    private fun <T> statementBlocks(responses: List<T>, message: (T) -> StatementResponse?): List<StatementBlock> =
        responses.mapNotNull(message)
            .map { StatementBlock(it.currencyCode, it.transactionList?.transactions.orEmpty()) }

    private data class StatementBlock(
        val currencyCode: String?,
        val transactions: List<Transaction>,
    )
}
