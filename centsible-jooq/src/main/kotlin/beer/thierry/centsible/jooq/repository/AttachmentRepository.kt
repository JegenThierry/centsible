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

private val ATTACHMENTS = DSL.table("transaction_attachments")
private val A_ID = DSL.field("id", UUID::class.java)
private val A_TXN = DSL.field("transaction_id", UUID::class.java)
private val A_USER = DSL.field("user_id", UUID::class.java)
private val A_FILENAME = DSL.field("filename", String::class.java)
private val A_CONTENT_TYPE = DSL.field("content_type", String::class.java)
private val A_SIZE = DSL.field("size_bytes", Long::class.java)
private val A_STORAGE_KEY = DSL.field("storage_key", String::class.java)
private val A_CREATED_AT = DSL.field("created_at", OffsetDateTime::class.java)

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

        dsl.insertInto(ATTACHMENTS)
            .set(A_ID, id)
            .set(A_TXN, transactionId)
            .set(A_USER, user.id)
            .set(A_FILENAME, filename)
            .set(A_CONTENT_TYPE, contentType)
            .set(A_SIZE, sizeBytes)
            .set(A_STORAGE_KEY, storageKey)
            .set(A_CREATED_AT, now)
            .execute()

        return AttachmentDTO(id, transactionId, filename, contentType, sizeBytes, now)
    }

    override fun listForTransaction(user: UserDTO, transactionId: UUID): List<AttachmentDTO> =
        dsl.select(A_ID, A_TXN, A_FILENAME, A_CONTENT_TYPE, A_SIZE, A_CREATED_AT)
            .from(ATTACHMENTS)
            .where(A_USER.eq(user.id).and(A_TXN.eq(transactionId)))
            .orderBy(A_CREATED_AT.asc())
            .fetch {
                AttachmentDTO(
                    id = it[A_ID]!!,
                    transactionId = it[A_TXN]!!,
                    filename = it[A_FILENAME]!!,
                    contentType = it[A_CONTENT_TYPE]!!,
                    sizeBytes = it[A_SIZE] ?: 0L,
                    createdAt = it[A_CREATED_AT]!!,
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
        return dsl.select(A_TXN, count)
            .from(ATTACHMENTS)
            .where(A_USER.eq(user.id).and(A_TXN.`in`(transactionIds)))
            .groupBy(A_TXN)
            .fetch()
            .associate { it[A_TXN]!! to (it[count] ?: 0) }
    }

    override fun fetch(user: UserDTO, attachmentId: UUID): AttachmentContent? =
        dsl.select(A_ID, A_TXN, A_FILENAME, A_CONTENT_TYPE, A_SIZE, A_STORAGE_KEY, A_CREATED_AT)
            .from(ATTACHMENTS)
            .where(A_USER.eq(user.id).and(A_ID.eq(attachmentId)))
            .fetchOne {
                AttachmentContent(
                    metadata = AttachmentDTO(
                        id = it[A_ID]!!,
                        transactionId = it[A_TXN]!!,
                        filename = it[A_FILENAME]!!,
                        contentType = it[A_CONTENT_TYPE]!!,
                        sizeBytes = it[A_SIZE] ?: 0L,
                        createdAt = it[A_CREATED_AT]!!,
                    ),
                    storageKey = it[A_STORAGE_KEY]!!,
                )
            }

    override fun delete(user: UserDTO, attachmentId: UUID): AttachmentContent? {
        val existing = fetch(user, attachmentId) ?: return null
        val deleted = dsl.deleteFrom(ATTACHMENTS)
            .where(A_USER.eq(user.id).and(A_ID.eq(attachmentId)))
            .execute()
        return if (deleted > 0) existing else null
    }
}
