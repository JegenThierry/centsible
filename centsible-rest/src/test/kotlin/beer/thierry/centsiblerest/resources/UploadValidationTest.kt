package beer.thierry.centsiblerest.resources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

class UploadValidationTest {

    private val allowed = setOf("image/png", "application/pdf")

    private val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val pdfBytes = "%PDF-1.4".toByteArray()

    private fun upload(
        bytes: ByteArray,
        contentType: String? = "application/octet-stream",
        filename: String = "test.bin",
    ): MultipartFile = MockMultipartFile("file", filename, contentType, bytes)

    @Test
    fun `rejects empty uploads with 400`() {
        val ex = assertThrows<ResponseStatusException> {
            upload(ByteArray(0)).validateContentType(allowed, maxBytes = 1000, tooLargeMessage = "too big")
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertTrue(ex.reason!!.contains("Empty"))
    }

    @Test
    fun `rejects over-size uploads with 413`() {
        val data = pngBytes + ByteArray(2000)
        val ex = assertThrows<ResponseStatusException> {
            upload(data).validateContentType(allowed, maxBytes = 100, tooLargeMessage = "too big — limit 100B")
        }
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.statusCode)
        assertEquals("too big — limit 100B", ex.reason)
    }

    @Test
    fun `rejects content whose detected type is not in the allowlist with 415`() {
        val ex = assertThrows<ResponseStatusException> {
            upload("just plain text".toByteArray()).validateContentType(
                allowed, maxBytes = 1000, tooLargeMessage = "too big",
                unsupportedTypeMessage = { "no parser for $it" },
            )
        }
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.statusCode)
        assertEquals("no parser for text/plain", ex.reason)
    }

    @Test
    fun `ignores a spoofed declared content type and detects the real one`() {
        val (type, bytes) = upload(pdfBytes, contentType = "image/png")
            .validateContentType(allowed, maxBytes = 1000, tooLargeMessage = "too big")
        assertEquals("application/pdf", type)
        assertTrue(bytes.contentEquals(pdfBytes))
    }

    @Test
    fun `returns the detected content type and bytes on success`() {
        val data = pngBytes + byteArrayOf(1, 2, 3)
        val (type, bytes) = upload(data, contentType = "Image/PNG")
            .validateContentType(allowed, maxBytes = 1000, tooLargeMessage = "too big")
        assertEquals("image/png", type)
        assertTrue(bytes.contentEquals(data))
    }
}
