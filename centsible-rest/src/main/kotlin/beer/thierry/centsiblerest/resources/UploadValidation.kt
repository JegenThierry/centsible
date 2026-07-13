package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.exceptions.LocalizedException
import org.apache.tika.Tika
import org.springframework.web.multipart.MultipartFile

private val TIKA = Tika()

/**
 * Validates a multipart upload by detecting its true content type from the file's own bytes (via
 * Apache Tika) and checking it against an allowlist. The client-declared `Content-Type` is ignored
 * for the decision — it is trivially spoofable — so whatever passes is genuinely one of
 * [allowedTypes].
 *
 * Throws [LocalizedException.BadRequest] (message-bundle keys, per ADR-0004) on every failure
 * mode; returns the detected content type and the file bytes on success so callers don't re-read
 * the stream.
 */
internal fun MultipartFile.validateContentType(
    allowedTypes: Set<String>,
    maxBytes: Long,
): Pair<String, ByteArray> {
    if (isEmpty) throw LocalizedException.BadRequest("error.upload.empty")
    if (size > maxBytes) throw LocalizedException.BadRequest("error.upload.tooLarge", maxBytes / (1024 * 1024))
    val data = bytes
    val detected = TIKA.detect(data).lowercase()
    if (detected !in allowedTypes) {
        throw LocalizedException.BadRequest("error.upload.unsupportedType", detected)
    }
    return detected to data
}
