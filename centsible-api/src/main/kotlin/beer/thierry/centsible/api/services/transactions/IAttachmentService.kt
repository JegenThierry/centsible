package beer.thierry.centsible.api.services.transactions

import beer.thierry.centsible.api.model.transaction.AttachmentDTO
import beer.thierry.centsible.api.model.transaction.AttachmentEnrichedDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.io.InputStream
import java.util.UUID

interface IAttachmentService {
    fun list(user: UserDTO, transactionId: UUID): List<AttachmentDTO>

    /** Paginated feed of attachments across all of the user's transactions, each enriched with its parent context. */
    fun listForUser(user: UserDTO, page: Int, size: Int): List<AttachmentEnrichedDTO>

    fun store(
        user: UserDTO,
        transactionId: UUID,
        filename: String,
        contentType: String,
        sizeBytes: Long,
        content: ByteArray,
    ): AttachmentDTO

    /** Opens the attachment's bytes for download, or null if absent; caller must close the returned stream. */
    fun open(user: UserDTO, attachmentId: UUID): AttachmentDownload?

    fun delete(user: UserDTO, attachmentId: UUID): Boolean
}

data class AttachmentDownload(
    val metadata: AttachmentDTO,
    val stream: InputStream,
)
