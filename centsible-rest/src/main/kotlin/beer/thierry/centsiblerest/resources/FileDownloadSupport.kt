package beer.thierry.centsiblerest.resources

import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

/**
 * Assembles a file-download response shared by the attachment and export endpoints: content type,
 * content length, and a `Content-Disposition` header whose filename is quote-stripped to keep the
 * header well-formed. Callers vary by [inline] vs. attachment disposition and by whether they set a
 * [cacheControl] value.
 */
fun <T : Resource> fileDownload(
    filename: String,
    contentType: MediaType,
    length: Long,
    resource: T,
    inline: Boolean,
    cacheControl: String? = null,
): ResponseEntity<T> {
    val disposition = if (inline) "inline" else "attachment"
    val builder = ResponseEntity.ok()
        .contentType(contentType)
        .contentLength(length)
        .header(HttpHeaders.CONTENT_DISPOSITION, """$disposition; filename="${filename.replace("\"", "")}"""")
    if (cacheControl != null) builder.header(HttpHeaders.CACHE_CONTROL, cacheControl)
    return builder.body(resource)
}
