package beer.thierry.centsiblerest.resources

import org.apache.tika.Tika
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

private val TIKA = Tika()

/**
 * Validates a multipart upload by detecting its true content type from the file's own bytes (via
 * Apache Tika) and checking it against an allowlist. The client-declared `Content-Type` is ignored
 * for the decision — it is trivially spoofable — so whatever passes is genuinely one of
 * [allowedTypes].
 *
 * Throws [ResponseStatusException] on every failure mode; returns the detected content type and the
 * file bytes on success so callers don't re-read the stream.
 */
internal fun MultipartFile.validateContentType(
    allowedTypes: Set<String>,
    maxBytes: Long,
    tooLargeMessage: String,
    unsupportedTypeMessage: (String) -> String = { "Unsupported type: $it" },
): Pair<String, ByteArray> {
    if (isEmpty) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file")
    if (size > maxBytes) throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, tooLargeMessage)
    val data = bytes
    val detected = TIKA.detect(data).lowercase()
    if (detected !in allowedTypes) {
        throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, unsupportedTypeMessage(detected))
    }
    return detected to data
}
