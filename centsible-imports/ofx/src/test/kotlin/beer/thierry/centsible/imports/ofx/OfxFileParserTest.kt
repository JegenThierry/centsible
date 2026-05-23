package beer.thierry.centsible.imports.ofx

import beer.thierry.centsible.imports.core.ParseHints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class OfxFileParserTest {

    private val parser = OfxFileParser()

    @Test
    fun `parses an OFX 1x SGML-style statement`() {
        val ofx = """
            OFXHEADER:100
            DATA:OFXSGML
            VERSION:102

            <OFX>
            <BANKMSGSRSV1>
            <STMTTRNRS>
            <STMTRS>
            <CURDEF>EUR
            <BANKACCTFROM>
            <ACCTID>123
            </BANKACCTFROM>
            <BANKTRANLIST>
            <STMTTRN>
            <TRNTYPE>DEBIT
            <DTPOSTED>20260115120000
            <TRNAMT>-3.50
            <FITID>20260115-001
            <NAME>Coffee shop
            </STMTTRN>
            <STMTTRN>
            <TRNTYPE>CREDIT
            <DTPOSTED>20260116
            <TRNAMT>2500.00
            <FITID>20260116-001
            <NAME>Salary
            </STMTTRN>
            </BANKTRANLIST>
            </STMTRS>
            </STMTTRNRS>
            </BANKMSGSRSV1>
            </OFX>
        """.trimIndent().toByteArray()

        val result = parser.parse(ofx, ParseHints(defaultCategoryId = 7L))

        assertEquals(2, result.rows.size)
        assertEquals("EUR", result.detectedCurrency)
        assertEquals(LocalDate.of(2026, 1, 15), result.rows[0].transactionDate)
        assertEquals(BigDecimal("3.50"), result.rows[0].amount)
        assertEquals("Coffee shop", result.rows[0].description)
        assertEquals(7L, result.rows[0].categoryId)
        assertEquals(BigDecimal("2500.00"), result.rows[1].amount)
    }

    @Test
    fun `sniff accepts files with OFXHEADER or OFX tag`() {
        assertTrue(parser.sniff("OFXHEADER:100\nfoo".toByteArray(), "x.ofx"))
        assertTrue(parser.sniff("<?xml?><OFX></OFX>".toByteArray(), "x.qfx"))
    }

    @Test
    fun `prefers NAME but falls back to MEMO when NAME is missing`() {
        val ofx = """
            OFXHEADER:100
            DATA:OFXSGML
            VERSION:102

            <OFX>
            <BANKMSGSRSV1>
            <STMTTRNRS>
            <STMTRS>
            <CURDEF>USD
            <BANKACCTFROM>
            <ACCTID>1
            </BANKACCTFROM>
            <BANKTRANLIST>
            <STMTTRN>
            <TRNTYPE>DEBIT
            <DTPOSTED>20260201
            <TRNAMT>-10.00
            <FITID>20260201-001
            <MEMO>Memo-only entry
            </STMTTRN>
            </BANKTRANLIST>
            </STMTRS>
            </STMTTRNRS>
            </BANKMSGSRSV1>
            </OFX>
        """.trimIndent().toByteArray()

        val result = parser.parse(ofx, ParseHints(defaultCategoryId = 1L))
        assertEquals(1, result.rows.size)
        assertEquals("Memo-only entry", result.rows[0].description)
        assertNotNull(result.rows[0].transactionDate)
    }
}
