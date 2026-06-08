package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.transaction.AttachmentContent
import beer.thierry.centsible.api.model.transaction.AttachmentDTO
import beer.thierry.centsible.api.model.transaction.AttachmentEnrichedDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IAttachmentRepository
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import beer.thierry.jooq.generated.tables.references.TRANSACTION_ATTACHMENTS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class AttachmentRepository(private val dsl: DSLContext) : IAttachmentRepository {

    override fun create(
        user: UserDTO,
        transactionId: UUID,
        filename: String,
        contentType: String,
        sizeBytes: Long,
        storageKey: String,
    ): AttachmentDTO {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()

        dsl.insertInto(TRANSACTION_ATTACHMENTS)
            .set(TRANSACTION_ATTACHMENTS.ID, id)
            .set(TRANSACTION_ATTACHMENTS.TRANSACTION_ID, transactionId)
            .set(TRANSACTION_ATTACHMENTS.USER_ID, user.id)
            .set(TRANSACTION_ATTACHMENTS.FILENAME, filename)
            .set(TRANSACTION_ATTACHMENTS.CONTENT_TYPE, contentType)
            .set(TRANSACTION_ATTACHMENTS.SIZE_BYTES, sizeBytes)
            .set(TRANSACTION_ATTACHMENTS.STORAGE_KEY, storageKey)
            .set(TRANSACTION_ATTACHMENTS.CREATED_AT, now)
            .execute()

        return AttachmentDTO(id, transactionId, filename, contentType, sizeBytes, now)
    }

    override fun listForTransaction(user: UserDTO, transactionId: UUID): List<AttachmentDTO> =
        dsl.select(
            TRANSACTION_ATTACHMENTS.ID,
            TRANSACTION_ATTACHMENTS.TRANSACTION_ID,
            TRANSACTION_ATTACHMENTS.FILENAME,
            TRANSACTION_ATTACHMENTS.CONTENT_TYPE,
            TRANSACTION_ATTACHMENTS.SIZE_BYTES,
            TRANSACTION_ATTACHMENTS.CREATED_AT,
        )
            .from(TRANSACTION_ATTACHMENTS)
            .where(TRANSACTION_ATTACHMENTS.USER_ID.eq(user.id).and(TRANSACTION_ATTACHMENTS.TRANSACTION_ID.eq(transactionId)))
            .orderBy(TRANSACTION_ATTACHMENTS.CREATED_AT.asc())
            .fetch {
                AttachmentDTO(
                    id = it[TRANSACTION_ATTACHMENTS.ID]!!,
                    transactionId = it[TRANSACTION_ATTACHMENTS.TRANSACTION_ID]!!,
                    filename = it[TRANSACTION_ATTACHMENTS.FILENAME]!!,
                    contentType = it[TRANSACTION_ATTACHMENTS.CONTENT_TYPE]!!,
                    sizeBytes = it[TRANSACTION_ATTACHMENTS.SIZE_BYTES] ?: 0L,
                    createdAt = it[TRANSACTION_ATTACHMENTS.CREATED_AT]!!,
                )
            }

    override fun listForUser(user: UserDTO, page: Int, size: Int): List<AttachmentEnrichedDTO> =
        dsl.select(
            TRANSACTION_ATTACHMENTS.ID,
            TRANSACTION_ATTACHMENTS.TRANSACTION_ID,
            TRANSACTION_ATTACHMENTS.FILENAME,
            TRANSACTION_ATTACHMENTS.CONTENT_TYPE,
            TRANSACTION_ATTACHMENTS.SIZE_BYTES,
            TRANSACTION_ATTACHMENTS.CREATED_AT,
            TRANSACTIONS.ACCOUNT_ID,
            TRANSACTIONS.DESCRIPTION,
            TRANSACTIONS.TRANSACTION_DATE,
        )
            .from(TRANSACTION_ATTACHMENTS)
            .join(TRANSACTIONS).on(TRANSACTIONS.ID.eq(TRANSACTION_ATTACHMENTS.TRANSACTION_ID))
            .where(TRANSACTION_ATTACHMENTS.USER_ID.eq(user.id))
            .orderBy(TRANSACTION_ATTACHMENTS.CREATED_AT.desc())
            .limit(size)
            .offset((page - 1) * size)
            .fetch {
                AttachmentEnrichedDTO(
                    id = it[TRANSACTION_ATTACHMENTS.ID]!!,
                    transactionId = it[TRANSACTION_ATTACHMENTS.TRANSACTION_ID]!!,
                    accountId = it[TRANSACTIONS.ACCOUNT_ID]!!,
                    filename = it[TRANSACTION_ATTACHMENTS.FILENAME]!!,
                    contentType = it[TRANSACTION_ATTACHMENTS.CONTENT_TYPE]!!,
                    sizeBytes = it[TRANSACTION_ATTACHMENTS.SIZE_BYTES] ?: 0L,
                    createdAt = it[TRANSACTION_ATTACHMENTS.CREATED_AT]!!,
                    transactionDescription = it[TRANSACTIONS.DESCRIPTION],
                    transactionDate = it[TRANSACTIONS.TRANSACTION_DATE]!!,
                )
            }

    override fun countByTransactionIds(user: UserDTO, transactionIds: Collection<UUID>): Map<UUID, Int> {
        if (transactionIds.isEmpty()) return emptyMap()
        val count = DSL.count()
        return dsl.select(TRANSACTION_ATTACHMENTS.TRANSACTION_ID, count)
            .from(TRANSACTION_ATTACHMENTS)
            .where(TRANSACTION_ATTACHMENTS.USER_ID.eq(user.id).and(TRANSACTION_ATTACHMENTS.TRANSACTION_ID.`in`(transactionIds)))
            .groupBy(TRANSACTION_ATTACHMENTS.TRANSACTION_ID)
            .fetch()
            .associate { it[TRANSACTION_ATTACHMENTS.TRANSACTION_ID]!! to (it[count] ?: 0) }
    }

    override fun fetch(user: UserDTO, attachmentId: UUID): AttachmentContent? =
        dsl.select(
            TRANSACTION_ATTACHMENTS.ID,
            TRANSACTION_ATTACHMENTS.TRANSACTION_ID,
            TRANSACTION_ATTACHMENTS.FILENAME,
            TRANSACTION_ATTACHMENTS.CONTENT_TYPE,
            TRANSACTION_ATTACHMENTS.SIZE_BYTES,
            TRANSACTION_ATTACHMENTS.STORAGE_KEY,
            TRANSACTION_ATTACHMENTS.CREATED_AT,
        )
            .from(TRANSACTION_ATTACHMENTS)
            .where(TRANSACTION_ATTACHMENTS.USER_ID.eq(user.id).and(TRANSACTION_ATTACHMENTS.ID.eq(attachmentId)))
            .fetchOne {
                AttachmentContent(
                    metadata = AttachmentDTO(
                        id = it[TRANSACTION_ATTACHMENTS.ID]!!,
                        transactionId = it[TRANSACTION_ATTACHMENTS.TRANSACTION_ID]!!,
                        filename = it[TRANSACTION_ATTACHMENTS.FILENAME]!!,
                        contentType = it[TRANSACTION_ATTACHMENTS.CONTENT_TYPE]!!,
                        sizeBytes = it[TRANSACTION_ATTACHMENTS.SIZE_BYTES] ?: 0L,
                        createdAt = it[TRANSACTION_ATTACHMENTS.CREATED_AT]!!,
                    ),
                    storageKey = it[TRANSACTION_ATTACHMENTS.STORAGE_KEY]!!,
                )
            }

    override fun delete(user: UserDTO, attachmentId: UUID): AttachmentContent? {
        val existing = fetch(user, attachmentId) ?: return null
        val deleted = dsl.deleteFrom(TRANSACTION_ATTACHMENTS)
            .where(TRANSACTION_ATTACHMENTS.USER_ID.eq(user.id).and(TRANSACTION_ATTACHMENTS.ID.eq(attachmentId)))
            .execute()
        return if (deleted > 0) existing else null
    }
}
