package beer.thierry.budgetplannerrest.resources

import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.Base64

private const val MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024

private data class MagicBytes(val offset: Int, val bytes: ByteArray)

private val IMAGE_SIGNATURES = mapOf(
    "image/png"  to listOf(MagicBytes(0, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))),
    "image/jpeg" to listOf(MagicBytes(0, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))),
    "image/gif"  to listOf(MagicBytes(0, byteArrayOf(0x47, 0x49, 0x46, 0x38))),
    "image/webp" to listOf(
        MagicBytes(0, byteArrayOf(0x52, 0x49, 0x46, 0x46)),
        MagicBytes(8, byteArrayOf(0x57, 0x45, 0x42, 0x50)),
    ),
)

// Magic-byte sniffing is what stops spoofed Content-Type from smuggling arbitrary binaries.
fun MultipartFile.toValidatedImageDataUrl(): String {
    if (isEmpty) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file")
    if (size > MAX_IMAGE_SIZE_BYTES) {
        throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image must be ≤ 2MB")
    }
    val declared = contentType?.lowercase()
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing content type")
    val signatures = IMAGE_SIGNATURES[declared]
        ?: throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported image type: $declared")

    val bytes = this.bytes
    if (!signatures.all { it.matches(bytes) }) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File contents do not match declared image type")
    }
    return "data:$declared;base64,${Base64.getEncoder().encodeToString(bytes)}"
}

private fun MagicBytes.matches(data: ByteArray): Boolean {
    if (data.size < offset + bytes.size) return false
    for (i in bytes.indices) {
        if (data[offset + i] != bytes[i]) return false
    }
    return true
}
