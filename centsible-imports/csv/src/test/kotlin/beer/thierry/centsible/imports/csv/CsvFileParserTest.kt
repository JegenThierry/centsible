package beer.thierry.centsible.imports.csv

import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.ParseHints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class CsvFileParserTest {

    private val parser = CsvFileParser()

    @Test
    fun `parses a simple comma CSV with single signed amount column`() {
        val csv = """
            Date,Description,Amount
            2026-01-15,Coffee shop,-3.50
            2026-01-16,Salary,2500.00
        """.trimIndent().toByteArray()

        val hints = ParseHints(
            defaultCategoryId = 42L,
            csvMapping = CsvColumnMapping(
                dateColumn = 0,
                descriptionColumn = 1,
                amountColumn = 2,
                dateFormat = "yyyy-MM-dd",
            ),
        )

        val result = parser.parse(csv, hints)

        assertEquals(2, result.rows.size)
        assertEquals(LocalDate.of(2026, 1, 15), result.rows[0].transactionDate)
        assertEquals(BigDecimal("3.50"), result.rows[0].amount)
        assertEquals(42L, result.rows[0].categoryId)
        assertEquals(BigDecimal("2500.00"), result.rows[1].amount)
    }

    @Test
    fun `probe returns header and sample rows`() {
        val csv = "Date;Description;Amount\n2026-01-01;Test;1.23\n2026-01-02;Other;4.56".toByteArray()
        val probe = parser.probe(csv, ParseHints())
        assertEquals(listOf("Date", "Description", "Amount"), probe.header)
        assertEquals(2, probe.sample.size)
        assertEquals(';', probe.detectedDialect.delimiter)
    }

    @Test
    fun `sniff accepts comma, semicolon, and tab delimited content`() {
        assertTrue(parser.sniff("a,b,c\n1,2,3".toByteArray(), "f.csv"))
        assertTrue(parser.sniff("a;b;c\n1;2;3".toByteArray(), "f.csv"))
        assertTrue(parser.sniff("a\tb\tc\n1\t2\t3".toByteArray(), "f.tsv"))
    }

    @Test
    fun `sniff rejects OFX content so it cannot shadow the OFX parser on a txt upload`() {
        val ofx = "OFXHEADER:100\nDATA:OFXSGML\n\n<OFX><BANKMSGSRSV1><STMTTRN><NAME>Shop, Inc</NAME>"
        assertFalse(parser.sniff(ofx.toByteArray(), "statement.txt"))
    }

    @Test
    fun `probe strips a UTF-8 BOM so the first header cell stays clean`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val csv = bom + "Date;Description;Amount\n2026-01-01;Test;1.23".toByteArray()

        val probe = parser.probe(csv, ParseHints())

        assertEquals(listOf("Date", "Description", "Amount"), probe.header)
        assertEquals("UTF-8", probe.detectedDialect.encoding)
    }

    @Test
    fun `parses a windows-1252 encoded file without mangling umlauts`() {
        val csv = "Datum;Beschreibung;Betrag\n15.01.2026;Bäckerei Müller;-3,50"
            .toByteArray(charset("windows-1252"))

        val hints = ParseHints(
            defaultCategoryId = 42L,
            csvMapping = CsvColumnMapping(
                dateColumn = 0,
                descriptionColumn = 1,
                amountColumn = 2,
                dateFormat = "dd.MM.yyyy",
                decimalSeparator = ',',
            ),
        )

        val result = parser.parse(csv, hints)

        assertEquals(1, result.rows.size)
        assertEquals("Bäckerei Müller", result.rows[0].description)
        assertEquals(BigDecimal("3.50"), result.rows[0].amount)
    }

    @Test
    fun `probe detects windows-1252 for non-UTF8 bytes`() {
        val csv = "Datum;Beschreibung;Betrag\n15.01.2026;Bäckerei;-3,50".toByteArray(charset("windows-1252"))
        val probe = parser.probe(csv, ParseHints())
        assertEquals("windows-1252", probe.detectedDialect.encoding)
        assertEquals(listOf("Datum", "Beschreibung", "Betrag"), probe.header)
    }

    @Test
    fun `parse reports an unterminated quote as a warning instead of throwing`() {
        val csv = "Date,Description,Amount\n2026-01-15,\"unterminated,-3.50\n".toByteArray()

        val hints = ParseHints(
            defaultCategoryId = 42L,
            csvMapping = CsvColumnMapping(
                dateColumn = 0,
                descriptionColumn = 1,
                amountColumn = 2,
                dateFormat = "yyyy-MM-dd",
            ),
        )

        val result = parser.parse(csv, hints)

        assertTrue(result.rows.isEmpty())
        assertEquals(listOf("csv.unparseable"), result.warnings.map { it.code })
        assertTrue(
            result.warnings.single().message.contains("EOF reached before encapsulated token finished"),
            "warning should carry the commons-csv reason, was '${result.warnings.single().message}'",
        )
    }

    @Test
    fun `probe reports an unterminated quote as a warning instead of throwing`() {
        val csv = "Date;Description;Amount\n2026-01-01;\"unterminated;1.23\n".toByteArray()

        val probe = parser.probe(csv, ParseHints())

        assertTrue(probe.header.isEmpty())
        assertTrue(probe.sample.isEmpty())
        assertEquals(listOf("csv.unparseable"), probe.warnings.map { it.code })
    }
}
