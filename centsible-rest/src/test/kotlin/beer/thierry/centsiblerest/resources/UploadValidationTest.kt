package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.exceptions.LocalizedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile

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
    fun `rejects empty uploads with a localized 400`() {
        val ex = assertThrows<LocalizedException.BadRequest> {
            upload(ByteArray(0)).validateContentType(allowed, maxBytes = 1000)
        }
        assertEquals("error.upload.empty", ex.messageKey)
    }

    @Test
    fun `rejects over-size uploads with the limit in MB as message arg`() {
        val data = pngBytes + ByteArray(3 * 1024 * 1024)
        val ex = assertThrows<LocalizedException.BadRequest> {
            upload(data).validateContentType(allowed, maxBytes = 2L * 1024 * 1024)
        }
        assertEquals("error.upload.tooLarge", ex.messageKey)
        assertEquals(2L, ex.args.single())
    }

    @Test
    fun `rejects content whose detected type is not in the allowlist`() {
        val ex = assertThrows<LocalizedException.BadRequest> {
            upload("just plain text".toByteArray()).validateContentType(allowed, maxBytes = 1000)
        }
        assertEquals("error.upload.unsupportedType", ex.messageKey)
        assertEquals("text/plain", ex.args.single())
    }

    @Test
    fun `ignores a spoofed declared content type and detects the real one`() {
        val (type, bytes) = upload(pdfBytes, contentType = "image/png")
            .validateContentType(allowed, maxBytes = 1000)
        assertEquals("application/pdf", type)
        assertTrue(bytes.contentEquals(pdfBytes))
    }

    @Test
    fun `returns the detected content type and bytes on success`() {
        val data = pngBytes + byteArrayOf(1, 2, 3)
        val (type, bytes) = upload(data, contentType = "Image/PNG")
            .validateContentType(allowed, maxBytes = 1000)
        assertEquals("image/png", type)
        assertTrue(bytes.contentEquals(data))
    }
}
