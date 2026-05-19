package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.transaction.AttachmentDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.transactions.IAttachmentService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

private const val MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024

private val ATTACHMENT_SIGNATURES: Map<String, List<MagicBytes>> = mapOf(
    "application/pdf" to listOf(MagicBytes(0, byteArrayOf(0x25, 0x50, 0x44, 0x46))),
    "image/jpeg" to listOf(MagicBytes(0, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))),
    "image/png" to listOf(MagicBytes(0, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))),
    "image/webp" to listOf(
        MagicBytes(0, byteArrayOf(0x52, 0x49, 0x46, 0x46)),
        MagicBytes(8, byteArrayOf(0x57, 0x45, 0x42, 0x50)),
    ),
)

@RequestMapping("/api/transactions/{transactionId}/attachments")
@RestController
class AttachmentsResource(private val service: IAttachmentService) {

    @GetMapping
    fun list(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<List<AttachmentDTO>> =
        ResponseEntity.ok(service.list(user, transactionId))

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @PathVariable transactionId: UUID,
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<AttachmentDTO> {
        val (declared, bytes) = file.validateAgainstSignatures(
            signatures = ATTACHMENT_SIGNATURES,
            maxBytes = MAX_ATTACHMENT_BYTES,
            tooLargeMessage = "Attachment must be ≤ 10MB",
        )
        val filename = sanitiseFilename(file.originalFilename ?: "attachment")
        val saved = service.store(user, transactionId, filename, declared, file.size, bytes)
        return ResponseEntity.ok(saved)
    }

    @GetMapping("/{attachmentId}")
    fun download(
        @PathVariable transactionId: UUID,
        @PathVariable attachmentId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<InputStreamResource> {
        val download = service.open(user, attachmentId) ?: return ResponseEntity.notFound().build()
        if (download.metadata.transactionId != transactionId) return ResponseEntity.notFound().build()

        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(download.metadata.contentType)
            contentLength = download.metadata.sizeBytes
            // `inline` lets the browser preview where possible; download still works.
            set(HttpHeaders.CONTENT_DISPOSITION,
                """inline; filename="${download.metadata.filename.replace("\"", "")}"""")
            cacheControl = "no-store"
        }
        return ResponseEntity.ok().headers(headers).body(InputStreamResource(download.stream))
    }

    @DeleteMapping("/{attachmentId}")
    fun delete(
        @PathVariable transactionId: UUID,
        @PathVariable attachmentId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<Void> =
        if (service.delete(user, attachmentId)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    private fun sanitiseFilename(name: String): String {
        val cleaned = name.replace(Regex("[\\r\\n\\t\\\\/]"), "_").trim()
        return if (cleaned.length <= 255) cleaned else cleaned.takeLast(255)
    }
}
