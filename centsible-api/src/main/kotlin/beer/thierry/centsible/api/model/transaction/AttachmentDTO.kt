package beer.thierry.centsible.api.model.transaction

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class AttachmentDTO(
    val id: UUID,
    val transactionId: UUID,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val createdAt: OffsetDateTime,
)

data class AttachmentContent(
    val metadata: AttachmentDTO,
    val storageKey: String,
)

data class AttachmentEnrichedDTO(
    val id: UUID,
    val transactionId: UUID,
    val accountId: UUID,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val createdAt: OffsetDateTime,
    val transactionDescription: String?,
    val transactionDate: LocalDate,
)
