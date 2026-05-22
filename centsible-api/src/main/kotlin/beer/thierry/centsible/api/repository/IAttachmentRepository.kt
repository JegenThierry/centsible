package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.transaction.AttachmentContent
import beer.thierry.centsible.api.model.transaction.AttachmentDTO
import beer.thierry.centsible.api.model.transaction.AttachmentEnrichedDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

interface IAttachmentRepository {
    fun create(
        user: UserDTO,
        transactionId: UUID,
        filename: String,
        contentType: String,
        sizeBytes: Long,
        storageKey: String,
    ): AttachmentDTO

    fun listForTransaction(user: UserDTO, transactionId: UUID): List<AttachmentDTO>

    fun listForUser(user: UserDTO, page: Int, size: Int): List<AttachmentEnrichedDTO>

    fun countByTransactionIds(user: UserDTO, transactionIds: Collection<UUID>): Map<UUID, Int>

    fun fetch(user: UserDTO, attachmentId: UUID): AttachmentContent?

    fun delete(user: UserDTO, attachmentId: UUID): AttachmentContent?
}
