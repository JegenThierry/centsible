package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.transaction.AttachmentDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.transactions.IAttachmentService
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

private const val MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024

private val ALLOWED_ATTACHMENT_TYPES = setOf("application/pdf", "image/jpeg", "image/png", "image/webp")

@RequestMapping("/api/transactions/{transactionId}/attachments")
@RestController
class AttachmentsResource(private val service: IAttachmentService) {

    private val log = LoggerFactory.getLogger(AttachmentsResource::class.java)

    @GetMapping
    fun list(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): List<AttachmentDTO> =
        service.list(user, transactionId)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @PathVariable transactionId: UUID,
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal user: UserDTO,
    ): AttachmentDTO {
        val (detected, bytes) = file.validateContentType(
            allowedTypes = ALLOWED_ATTACHMENT_TYPES,
            maxBytes = MAX_ATTACHMENT_BYTES,
        )
        val filename = sanitiseFilename(file.originalFilename ?: "attachment")
        val saved = service.store(user, transactionId, filename, detected, file.size, bytes)
        log.info(
            "Uploaded attachment id={} transactionId={} userId={} contentType={} sizeBytes={}",
            saved.id, transactionId, user.id, detected, file.size,
        )
        return saved
    }

    @GetMapping("/{attachmentId}")
    fun download(
        @PathVariable transactionId: UUID,
        @PathVariable attachmentId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<InputStreamResource> {
        val download = service.open(user, attachmentId) ?: return ResponseEntity.notFound().build()
        if (download.metadata.transactionId != transactionId) {
            download.stream.close()
            return ResponseEntity.notFound().build()
        }
        return fileDownload(
            filename = download.metadata.filename,
            contentType = MediaType.parseMediaType(download.metadata.contentType),
            length = download.metadata.sizeBytes,
            resource = InputStreamResource(download.stream),
            inline = true,
            cacheControl = "no-store",
        )
    }

    @DeleteMapping("/{attachmentId}")
    fun delete(
        @PathVariable transactionId: UUID,
        @PathVariable attachmentId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<Void> {
        val deleted = service.delete(user, attachmentId)
        if (deleted) log.info("Deleted attachment id={} transactionId={} userId={}", attachmentId, transactionId, user.id)
        return deleted.toDeleteResponse()
    }

    private fun sanitiseFilename(name: String): String {
        val cleaned = name.replace(Regex("[\\r\\n\\t\\\\/]"), "_").trim()
        return if (cleaned.length <= 255) cleaned else cleaned.takeLast(255)
    }
}
