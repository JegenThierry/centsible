package beer.thierry.centsible.api.services.transactions

import beer.thierry.centsible.api.model.transaction.AttachmentDTO
import beer.thierry.centsible.api.model.transaction.AttachmentEnrichedDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.io.InputStream
import java.util.UUID

interface IAttachmentService {
    fun list(user: UserDTO, transactionId: UUID): List<AttachmentDTO>

    fun listForUser(user: UserDTO, page: Int, size: Int): List<AttachmentEnrichedDTO>

    fun store(
        user: UserDTO,
        transactionId: UUID,
        filename: String,
        contentType: String,
        sizeBytes: Long,
        content: ByteArray,
    ): AttachmentDTO

    fun open(user: UserDTO, attachmentId: UUID): AttachmentDownload?

    fun delete(user: UserDTO, attachmentId: UUID): Boolean
}

data class AttachmentDownload(
    val metadata: AttachmentDTO,
    val stream: InputStream,
)
