package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.savedfilter.SavedFilterDTO
import beer.thierry.centsible.api.model.savedfilter.SavedFilterForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ISavedFilterRepository
import beer.thierry.jooq.generated.tables.references.SAVED_TRANSACTION_FILTERS
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class SavedFilterRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : ISavedFilterRepository {

    override fun fetchAll(authenticatedUser: UserDTO): List<SavedFilterDTO> =
        dsl.selectFrom(SAVED_TRANSACTION_FILTERS)
            .where(SAVED_TRANSACTION_FILTERS.USER_ID.eq(authenticatedUser.id))
            .orderBy(DSL.lower(SAVED_TRANSACTION_FILTERS.NAME).asc())
            .fetch { mapToDTO(it) }

    override fun fetchById(authenticatedUser: UserDTO, id: Long): SavedFilterDTO? =
        dsl.selectFrom(SAVED_TRANSACTION_FILTERS)
            .where(SAVED_TRANSACTION_FILTERS.USER_ID.eq(authenticatedUser.id).and(SAVED_TRANSACTION_FILTERS.ID.eq(id)))
            .fetchOne { mapToDTO(it) }

    override fun existsByName(authenticatedUser: UserDTO, name: String, excludeId: Long?): Boolean {
        var condition = SAVED_TRANSACTION_FILTERS.USER_ID.eq(authenticatedUser.id)
            .and(DSL.lower(SAVED_TRANSACTION_FILTERS.NAME).eq(name.trim().lowercase()))
        if (excludeId != null) {
            condition = condition.and(SAVED_TRANSACTION_FILTERS.ID.ne(excludeId))
        }
        return dsl.fetchExists(dsl.selectOne().from(SAVED_TRANSACTION_FILTERS).where(condition))
    }

    override fun create(authenticatedUser: UserDTO, form: SavedFilterForm): SavedFilterDTO {
        val now = OffsetDateTime.now()
        val id = dsl.insertInto(SAVED_TRANSACTION_FILTERS)
            .set(SAVED_TRANSACTION_FILTERS.USER_ID, authenticatedUser.id)
            .set(SAVED_TRANSACTION_FILTERS.NAME, form.name.trim())
            .set(SAVED_TRANSACTION_FILTERS.FILTERS, toJson(form.filters))
            .set(SAVED_TRANSACTION_FILTERS.CREATED_AT, now)
            .set(SAVED_TRANSACTION_FILTERS.MODIFIED_AT, now)
            .returning(SAVED_TRANSACTION_FILTERS.ID)
            .fetchOne()?.get(SAVED_TRANSACTION_FILTERS.ID)
            ?: throw IllegalStateException("Failed to create saved filter")
        return fetchById(authenticatedUser, id)
            ?: throw IllegalStateException("Created saved filter could not be retrieved")
    }

    override fun update(authenticatedUser: UserDTO, id: Long, form: SavedFilterForm): SavedFilterDTO? {
        val affected = dsl.update(SAVED_TRANSACTION_FILTERS)
            .set(SAVED_TRANSACTION_FILTERS.NAME, form.name.trim())
            .set(SAVED_TRANSACTION_FILTERS.FILTERS, toJson(form.filters))
            .set(SAVED_TRANSACTION_FILTERS.MODIFIED_AT, OffsetDateTime.now())
            .where(SAVED_TRANSACTION_FILTERS.USER_ID.eq(authenticatedUser.id).and(SAVED_TRANSACTION_FILTERS.ID.eq(id)))
            .execute()
        return if (affected > 0) fetchById(authenticatedUser, id) else null
    }

    override fun delete(authenticatedUser: UserDTO, id: Long): Boolean =
        dsl.deleteFrom(SAVED_TRANSACTION_FILTERS)
            .where(SAVED_TRANSACTION_FILTERS.USER_ID.eq(authenticatedUser.id).and(SAVED_TRANSACTION_FILTERS.ID.eq(id)))
            .execute() > 0

    private fun toJson(filters: Map<String, Any?>): JSONB =
        JSONB.valueOf(objectMapper.writeValueAsString(filters))

    private fun parseFilters(jsonb: JSONB?): Map<String, Any?> {
        val raw = jsonb?.data() ?: return emptyMap()
        return try {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(raw, Map::class.java) as Map<String, Any?>
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun mapToDTO(record: Record): SavedFilterDTO =
        SavedFilterDTO(
            id = record[SAVED_TRANSACTION_FILTERS.ID],
            name = record[SAVED_TRANSACTION_FILTERS.NAME] ?: "",
            filters = parseFilters(record[SAVED_TRANSACTION_FILTERS.FILTERS]),
            createdAt = record[SAVED_TRANSACTION_FILTERS.CREATED_AT],
            modifiedAt = record[SAVED_TRANSACTION_FILTERS.MODIFIED_AT],
        )
}
