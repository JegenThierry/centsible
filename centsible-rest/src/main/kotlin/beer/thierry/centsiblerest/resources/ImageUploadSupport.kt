package beer.thierry.centsiblerest.resources

import org.springframework.web.multipart.MultipartFile
import java.util.Base64

private const val MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024

private val ALLOWED_IMAGE_TYPES = setOf("image/png", "image/jpeg", "image/gif", "image/webp")

fun MultipartFile.toValidatedImageDataUrl(): String {
    val (detected, bytes) = validateContentType(
        allowedTypes = ALLOWED_IMAGE_TYPES,
        maxBytes = MAX_IMAGE_SIZE_BYTES,
        tooLargeMessage = "Image must be ≤ 2MB",
        unsupportedTypeMessage = { "Unsupported image type: $it" },
    )
    return "data:$detected;base64,${Base64.getEncoder().encodeToString(bytes)}"
}
