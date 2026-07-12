package beer.thierry.centsible.core.services.transactions

import beer.thierry.centsible.api.model.transaction.AttachmentDTO
import beer.thierry.centsible.api.model.transaction.AttachmentEnrichedDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IAttachmentRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.services.transactions.AttachmentDownload
import beer.thierry.centsible.api.services.transactions.IAttachmentService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class AttachmentService(
    private val attachments: IAttachmentRepository,
    private val transactions: ITransactionRepository,
    @Value("\${attachments.directory:./data/attachments}") private val attachmentsDirectory: String,
) : IAttachmentService {

    private val log = LoggerFactory.getLogger(AttachmentService::class.java)

    private val root: Path by lazy {
        val path = Path.of(attachmentsDirectory).toAbsolutePath().normalize()
        Files.createDirectories(path)
        path
    }

    override fun list(user: UserDTO, transactionId: UUID): List<AttachmentDTO> {
        transactions.fetchTransactionById(transactionId, user)
        return attachments.listForTransaction(user, transactionId)
    }

    override fun listForUser(user: UserDTO, page: Int, size: Int): List<AttachmentEnrichedDTO> {
        require(page > 0) { "page must be > 0" }
        require(size > 0) { "size must be > 0" }
        return attachments.listForUser(user, page, size)
    }

    @Transactional
    override fun store(
        user: UserDTO,
        transactionId: UUID,
        filename: String,
        contentType: String,
        sizeBytes: Long,
        content: ByteArray,
    ): AttachmentDTO {
        transactions.fetchTransactionById(transactionId, user)

        val ext = sanitisedExtension(filename, contentType)
        val storageKey = "${user.id}/${transactionId}/${UUID.randomUUID()}$ext"
        val destination = resolve(storageKey)
        Files.createDirectories(destination.parent)
        try {
            Files.copy(content.inputStream(), destination, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            log.error(
                "Failed to write attachment to disk userId={} transactionId={} sizeBytes={}",
                user.id, transactionId, sizeBytes, e,
            )
            throw e
        }

        return try {
            val created = attachments.create(user, transactionId, filename, contentType, sizeBytes, storageKey)
            log.info(
                "Stored attachment id={} userId={} transactionId={} contentType={} sizeBytes={}",
                created.id, user.id, transactionId, contentType, sizeBytes,
            )
            created
        } catch (e: Exception) {
            log.error(
                "Failed to persist attachment metadata; rolling back file userId={} transactionId={}",
                user.id, transactionId, e,
            )
            runCatching { Files.deleteIfExists(destination) }
                .onFailure { log.warn("Failed to remove orphan attachment file {}", storageKey, it) }
            throw e
        }
    }

    override fun open(user: UserDTO, attachmentId: UUID): AttachmentDownload? {
        val content = attachments.fetch(user, attachmentId) ?: return null
        val path = resolve(content.storageKey)
        return try {
            AttachmentDownload(content.metadata, Files.newInputStream(path))
        } catch (_: NoSuchFileException) {
            log.warn("Attachment {} missing on disk at {}", attachmentId, path)
            null
        }
    }

    @Transactional
    override fun delete(user: UserDTO, attachmentId: UUID): Boolean {
        val removed = attachments.delete(user, attachmentId) ?: return false
        runCatching {
            Files.deleteIfExists(resolve(removed.storageKey))
        }.onFailure { log.warn("Failed to remove attachment file {}", removed.storageKey, it) }
        log.info("Deleted attachment id={} userId={}", attachmentId, user.id)
        return true
    }

    private fun resolve(storageKey: String): Path {
        val candidate = root.resolve(storageKey).normalize()
        require(candidate.startsWith(root)) { "Attachment path escapes storage root" }
        return candidate
    }

    private fun sanitisedExtension(filename: String, contentType: String): String {
        val raw = filename.substringAfterLast('.', "").lowercase()
        val byContent = when (contentType.lowercase()) {
            "application/pdf" -> "pdf"
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> ""
        }
        val ext = if (raw.matches(Regex("^[a-z0-9]{1,5}$"))) raw else byContent
        return if (ext.isBlank()) "" else ".$ext"
    }
}
