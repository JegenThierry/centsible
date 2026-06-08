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

    /** All of [user]'s attachments, newest first, enriched with parent-transaction context; [page] is 1-based. */
    fun listForUser(user: UserDTO, page: Int, size: Int): List<AttachmentEnrichedDTO>

    /** Attachment counts keyed by transaction id; ids with no attachments are absent from the map. */
    fun countByTransactionIds(user: UserDTO, transactionIds: Collection<UUID>): Map<UUID, Int>

    /** Metadata plus [AttachmentContent.storageKey] for a download, or null if not found / not owned by [user]. */
    fun fetch(user: UserDTO, attachmentId: UUID): AttachmentContent?

    /** Removes the row and returns its [AttachmentContent] so the caller can delete the backing file; null if absent / not owned. */
    fun delete(user: UserDTO, attachmentId: UUID): AttachmentContent?
}
