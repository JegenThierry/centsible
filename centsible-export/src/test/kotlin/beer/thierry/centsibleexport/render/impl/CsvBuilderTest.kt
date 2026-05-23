package beer.thierry.centsibleexport.render.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
        // RFC 4180: cells with comma or quote are wrapped, embedded quotes doubled.
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
}
