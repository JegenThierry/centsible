package beer.thierry.centsible.imports.csv

import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.ParseHints
import org.junit.jupiter.api.Assertions.assertEquals
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
}
