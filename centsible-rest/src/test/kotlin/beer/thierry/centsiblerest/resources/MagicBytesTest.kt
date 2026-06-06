package beer.thierry.centsiblerest.resources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

class MagicBytesTest {

    private val pngSignature = MagicBytes(
        offset = 0,
        bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
    )
    private val signatures = mapOf("image/png" to listOf(pngSignature))

    private fun upload(
        bytes: ByteArray,
        contentType: String? = "image/png",
        filename: String = "test.png",
    ): MultipartFile = MockMultipartFile("file", filename, contentType, bytes)

    @Test
    fun `MagicBytes matches when prefix bytes line up at the given offset`() {
        val data = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(10)
        assertTrue(pngSignature.matches(data))
    }

    @Test
    fun `MagicBytes returns false when data is shorter than offset plus bytes`() {
        assertTrue(!pngSignature.matches(ByteArray(3)))
    }

    @Test
    fun `MagicBytes returns false when bytes do not match`() {
        assertTrue(!pngSignature.matches(ByteArray(8) { 0 }))
    }

    @Test
    fun `validateAgainstSignatures rejects empty uploads with 400`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            upload(ByteArray(0)).validateAgainstSignatures(
                signatures, maxBytes = 1000, tooLargeMessage = "too big",
            )
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertTrue(ex.reason!!.contains("Empty"))
    }

    @Test
    fun `validateAgainstSignatures rejects over-size uploads with 413`() {
        val data = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(2000)
        val ex = assertThrows(ResponseStatusException::class.java) {
            upload(data).validateAgainstSignatures(
                signatures, maxBytes = 100, tooLargeMessage = "too big — limit 100B",
            )
        }
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.statusCode)
        assertEquals("too big — limit 100B", ex.reason)
    }

    @Test
    fun `validateAgainstSignatures rejects unknown declared content type with 415`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            upload(byteArrayOf(0, 1, 2), contentType = "application/x-mystery")
                .validateAgainstSignatures(
                    signatures, maxBytes = 1000,
                    tooLargeMessage = "too big",
                    unsupportedTypeMessage = { "no parser for $it" },
                )
        }
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.statusCode)
        assertEquals("no parser for application/x-mystery", ex.reason)
    }

    @Test
    fun `validateAgainstSignatures rejects missing content type with 400`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            upload(byteArrayOf(0, 1, 2), contentType = null)
                .validateAgainstSignatures(signatures, 1000, "too big")
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `validateAgainstSignatures rejects spoofed content type when magic bytes don't match`() {
        // Claims image/png but body is not a PNG.
        val ex = assertThrows(ResponseStatusException::class.java) {
            upload(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
                .validateAgainstSignatures(signatures, 1000, "too big")
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertTrue(ex.reason!!.contains("do not match"))
    }

    @Test
    fun `validateAgainstSignatures returns lowercased content type and bytes on success`() {
        val data = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + byteArrayOf(1, 2, 3)
        val (type, bytes) = upload(data, contentType = "Image/PNG")
            .validateAgainstSignatures(signatures, 1000, "too big")
        assertEquals("image/png", type)
        assertTrue(bytes.contentEquals(data))
    }
}
