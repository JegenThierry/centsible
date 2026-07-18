package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.tag.TagDTO
import beer.thierry.centsible.api.model.tag.TagForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ITagRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.TAGS
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import beer.thierry.jooq.generated.tables.references.TRANSACTION_TAGS
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class TagRepository(private val dsl: DSLContext) : ITagRepository {

    override fun fetchAll(authenticatedUser: UserDTO): List<TagDTO> =
        dsl.selectFrom(TAGS)
            .where(TAGS.USER_ID.eq(authenticatedUser.id))
            .orderBy(DSL.lower(TAGS.NAME).asc())
            .fetch { mapToDTO(it) }

    override fun fetchById(authenticatedUser: UserDTO, id: Long): TagDTO? =
        dsl.selectFrom(TAGS)
            .where(TAGS.USER_ID.eq(authenticatedUser.id).and(TAGS.ID.eq(id)))
            .fetchOne { mapToDTO(it) }

    override fun existsByName(authenticatedUser: UserDTO, name: String, excludeId: Long?): Boolean {
        var condition = TAGS.USER_ID.eq(authenticatedUser.id)
            .and(DSL.lower(TAGS.NAME).eq(name.trim().lowercase()))
        if (excludeId != null) {
            condition = condition.and(TAGS.ID.ne(excludeId))
        }
        return dsl.fetchExists(dsl.selectOne().from(TAGS).where(condition))
    }

    override fun create(authenticatedUser: UserDTO, form: TagForm): TagDTO {
        val now = OffsetDateTime.now()
        val id = dsl.insertInto(TAGS)
            .set(TAGS.USER_ID, authenticatedUser.id)
            .set(TAGS.NAME, form.name.trim())
            .set(TAGS.COLOR, form.color)
            .set(TAGS.CREATED_AT, now)
            .set(TAGS.MODIFIED_AT, now)
            .returning(TAGS.ID)
            .fetchOne()?.get(TAGS.ID)
            ?: throw IllegalStateException("Failed to create tag")
        return fetchById(authenticatedUser, id)
            ?: throw IllegalStateException("Created tag could not be retrieved")
    }

    override fun update(authenticatedUser: UserDTO, id: Long, form: TagForm): TagDTO? {
        val affected = dsl.update(TAGS)
            .set(TAGS.NAME, form.name.trim())
            .set(TAGS.COLOR, form.color)
            .set(TAGS.MODIFIED_AT, OffsetDateTime.now())
            .where(TAGS.USER_ID.eq(authenticatedUser.id).and(TAGS.ID.eq(id)))
            .execute()
        return if (affected > 0) fetchById(authenticatedUser, id) else null
    }

    override fun delete(authenticatedUser: UserDTO, id: Long): Boolean =
        dsl.deleteFrom(TAGS)
            .where(TAGS.USER_ID.eq(authenticatedUser.id).and(TAGS.ID.eq(id)))
            .execute() > 0

    override fun fetchTagsForTransaction(authenticatedUser: UserDTO, transactionId: UUID): List<TagDTO> =
        dsl.select(TAGS.ID, TAGS.NAME, TAGS.COLOR, TAGS.CREATED_AT, TAGS.MODIFIED_AT)
            .from(TAGS)
            .join(TRANSACTION_TAGS).on(TRANSACTION_TAGS.TAG_ID.eq(TAGS.ID))
            .where(TRANSACTION_TAGS.TRANSACTION_ID.eq(transactionId).and(TAGS.USER_ID.eq(authenticatedUser.id)))
            .orderBy(DSL.lower(TAGS.NAME).asc())
            .fetch { mapToDTO(it) }

    override fun fetchTagsByTransactionIds(
        authenticatedUser: UserDTO,
        transactionIds: List<UUID>,
    ): Map<UUID, List<TagDTO>> {
        if (transactionIds.isEmpty()) return emptyMap()
        return dsl.select(
            TRANSACTION_TAGS.TRANSACTION_ID,
            TAGS.ID, TAGS.NAME, TAGS.COLOR, TAGS.CREATED_AT, TAGS.MODIFIED_AT,
        )
            .from(TRANSACTION_TAGS)
            .join(TAGS).on(TAGS.ID.eq(TRANSACTION_TAGS.TAG_ID))
            .where(TRANSACTION_TAGS.TRANSACTION_ID.`in`(transactionIds).and(TAGS.USER_ID.eq(authenticatedUser.id)))
            .orderBy(DSL.lower(TAGS.NAME).asc())
            .filter { it[TRANSACTION_TAGS.TRANSACTION_ID] != null }
            .groupBy({ it[TRANSACTION_TAGS.TRANSACTION_ID]!! }, ::mapToDTO)
    }

    override fun setTransactionTags(authenticatedUser: UserDTO, transactionId: UUID, tagIds: List<Long>): Boolean {
        val ownsTransaction = dsl.fetchExists(
            dsl.selectOne()
                .from(TRANSACTIONS)
                .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
                .where(TRANSACTIONS.ID.eq(transactionId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
        )
        if (!ownsTransaction) return false

        dsl.deleteFrom(TRANSACTION_TAGS).where(TRANSACTION_TAGS.TRANSACTION_ID.eq(transactionId)).execute()
        if (tagIds.isNotEmpty()) {
            val insert = dsl.insertInto(TRANSACTION_TAGS, TRANSACTION_TAGS.TRANSACTION_ID, TRANSACTION_TAGS.TAG_ID)
            tagIds.distinct().forEach { tagId -> insert.values(transactionId, tagId) }
            insert.execute()
        }
        return true
    }

    override fun addTagsToTransactions(
        authenticatedUser: UserDTO,
        transactionIds: Collection<UUID>,
        tagIds: Collection<Long>,
    ): Int {
        if (transactionIds.isEmpty() || tagIds.isEmpty()) return 0
        val ownedTransactions = dsl.select(TRANSACTIONS.ID)
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(TRANSACTIONS.ID.`in`(transactionIds).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
            .fetch(TRANSACTIONS.ID)
        val ownedTags = dsl.select(TAGS.ID)
            .from(TAGS)
            .where(TAGS.ID.`in`(tagIds).and(TAGS.USER_ID.eq(authenticatedUser.id)))
            .fetch(TAGS.ID)
        if (ownedTransactions.isEmpty() || ownedTags.isEmpty()) return 0

        val insert = dsl.insertInto(TRANSACTION_TAGS, TRANSACTION_TAGS.TRANSACTION_ID, TRANSACTION_TAGS.TAG_ID)
        ownedTransactions.forEach { txId -> ownedTags.forEach { tagId -> insert.values(txId, tagId) } }
        return insert.onConflict(TRANSACTION_TAGS.TRANSACTION_ID, TRANSACTION_TAGS.TAG_ID).doNothing().execute()
    }

    private fun mapToDTO(record: Record): TagDTO =
        TagDTO(
            id = record[TAGS.ID],
            name = record[TAGS.NAME] ?: "",
            color = record[TAGS.COLOR] ?: "#6b7280",
            createdAt = record[TAGS.CREATED_AT],
            modifiedAt = record[TAGS.MODIFIED_AT],
        )
}
