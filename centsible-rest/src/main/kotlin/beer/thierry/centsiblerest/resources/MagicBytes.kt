package beer.thierry.centsiblerest.resources

import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

internal data class MagicBytes(val offset: Int, val bytes: ByteArray) {
    fun matches(data: ByteArray): Boolean {
        if (data.size < offset + bytes.size) return false
        for (i in bytes.indices) {
            if (data[offset + i] != bytes[i]) return false
        }
        return true
    }
}

/**
 * Validates a multipart upload against a magic-byte signature table. Spoofing the `Content-Type`
 * is trivial, so we sniff the first few bytes too — anything that gets past this check is at least
 * what its headers claim it is.
 *
 * Throws [ResponseStatusException] on every failure mode; returns the file bytes on success so
 * callers don't re-read the stream.
 */
internal fun MultipartFile.validateAgainstSignatures(
    signatures: Map<String, List<MagicBytes>>,
    maxBytes: Long,
    tooLargeMessage: String,
    unsupportedTypeMessage: (String) -> String = { "Unsupported type: $it" },
): Pair<String, ByteArray> {
    if (isEmpty) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file")
    if (size > maxBytes) throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, tooLargeMessage)
    val declared = contentType?.lowercase()
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing content type")
    val expected = signatures[declared]
        ?: throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, unsupportedTypeMessage(declared))
    val data = bytes
    if (!expected.all { it.matches(data) }) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File contents do not match declared type")
    }
    return declared to data
}
