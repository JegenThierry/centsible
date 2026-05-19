package beer.thierry.centsiblerest.resources

import org.springframework.web.multipart.MultipartFile
import java.util.Base64

private const val MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024

private val IMAGE_SIGNATURES = mapOf(
    "image/png"  to listOf(MagicBytes(0, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))),
    "image/jpeg" to listOf(MagicBytes(0, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))),
    "image/gif"  to listOf(MagicBytes(0, byteArrayOf(0x47, 0x49, 0x46, 0x38))),
    "image/webp" to listOf(
        MagicBytes(0, byteArrayOf(0x52, 0x49, 0x46, 0x46)),
        MagicBytes(8, byteArrayOf(0x57, 0x45, 0x42, 0x50)),
    ),
)

fun MultipartFile.toValidatedImageDataUrl(): String {
    val (declared, bytes) = validateAgainstSignatures(
        signatures = IMAGE_SIGNATURES,
        maxBytes = MAX_IMAGE_SIZE_BYTES,
        tooLargeMessage = "Image must be ≤ 2MB",
        unsupportedTypeMessage = { "Unsupported image type: $it" },
    )
    return "data:$declared;base64,${Base64.getEncoder().encodeToString(bytes)}"
}
