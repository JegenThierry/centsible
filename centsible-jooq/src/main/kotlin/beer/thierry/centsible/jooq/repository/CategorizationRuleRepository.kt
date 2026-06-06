package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.categorization.CategorizationRuleDTO
import beer.thierry.centsible.api.model.categorization.CategorizationRuleForm
import beer.thierry.centsible.api.model.categorization.MatchType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ICategorizationRuleRepository
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.CATEGORIZATION_RULES
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class CategorizationRuleRepository(private val dsl: DSLContext) : ICategorizationRuleRepository {

    override fun fetchAll(user: UserDTO): List<CategorizationRuleDTO> =
        baseSelect()
            .where(CATEGORIZATION_RULES.USER_ID.eq(user.id))
            .orderBy(CATEGORIZATION_RULES.PRIORITY.desc(), CATEGORIZATION_RULES.CREATED_AT.asc())
            .fetch { mapRule(it) }

    override fun fetchById(user: UserDTO, id: UUID): CategorizationRuleDTO? =
        baseSelect()
            .where(CATEGORIZATION_RULES.ID.eq(id).and(CATEGORIZATION_RULES.USER_ID.eq(user.id)))
            .fetchOne { mapRule(it) }

    override fun create(user: UserDTO, form: CategorizationRuleForm): CategorizationRuleDTO {
        val now = OffsetDateTime.now()
        val id = dsl.insertInto(
            CATEGORIZATION_RULES,
            CATEGORIZATION_RULES.USER_ID, CATEGORIZATION_RULES.MATCH_TYPE, CATEGORIZATION_RULES.PATTERN,
            CATEGORIZATION_RULES.CATEGORY_ID, CATEGORIZATION_RULES.PRIORITY,
            CATEGORIZATION_RULES.CREATED_AT, CATEGORIZATION_RULES.MODIFIED_AT,
        )
            .values(user.id, form.matchType.value, form.pattern, form.categoryId, form.priority, now, now)
            .returning(CATEGORIZATION_RULES.ID)
            .fetchOne()?.get(CATEGORIZATION_RULES.ID)
            ?: throw IllegalStateException("Failed to persist categorization rule")
        return fetchById(user, id) ?: throw IllegalStateException("Persisted rule could not be re-read")
    }

    override fun update(user: UserDTO, id: UUID, form: CategorizationRuleForm): CategorizationRuleDTO? {
        val updated = dsl.update(CATEGORIZATION_RULES)
            .set(CATEGORIZATION_RULES.MATCH_TYPE, form.matchType.value)
            .set(CATEGORIZATION_RULES.PATTERN, form.pattern)
            .set(CATEGORIZATION_RULES.CATEGORY_ID, form.categoryId)
            .set(CATEGORIZATION_RULES.PRIORITY, form.priority)
            .set(CATEGORIZATION_RULES.MODIFIED_AT, OffsetDateTime.now())
            .where(CATEGORIZATION_RULES.ID.eq(id).and(CATEGORIZATION_RULES.USER_ID.eq(user.id)))
            .execute()
        return if (updated == 0) null else fetchById(user, id)
    }

    override fun delete(user: UserDTO, id: UUID): Boolean =
        dsl.deleteFrom(CATEGORIZATION_RULES)
            .where(CATEGORIZATION_RULES.ID.eq(id).and(CATEGORIZATION_RULES.USER_ID.eq(user.id)))
            .execute() > 0

    private fun baseSelect() =
        dsl.select(
            CATEGORIZATION_RULES.ID, CATEGORIZATION_RULES.MATCH_TYPE, CATEGORIZATION_RULES.PATTERN,
            CATEGORIZATION_RULES.PRIORITY, CATEGORIZATION_RULES.CREATED_AT, CATEGORIZATION_RULES.MODIFIED_AT,
            CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.ICON, CATEGORIES.COLOR, CATEGORIES.TYPE, CATEGORIES.USER_ID,
        )
            .from(CATEGORIZATION_RULES)
            .join(CATEGORIES).on(CATEGORIES.ID.eq(CATEGORIZATION_RULES.CATEGORY_ID))

    private fun mapRule(r: Record): CategorizationRuleDTO = CategorizationRuleDTO(
        id = r[CATEGORIZATION_RULES.ID]!!,
        matchType = MatchType.fromValue(r[CATEGORIZATION_RULES.MATCH_TYPE]!!),
        pattern = r[CATEGORIZATION_RULES.PATTERN]!!,
        category = CategoryDTO(
            id = r[CATEGORIES.ID],
            name = r[CATEGORIES.NAME],
            icon = r[CATEGORIES.ICON],
            color = r[CATEGORIES.COLOR],
            type = CategoryType.fromValue(r[CATEGORIES.TYPE]!!),
            isSystem = r[CATEGORIES.USER_ID] == null,
        ),
        priority = r[CATEGORIZATION_RULES.PRIORITY]!!,
        createdAt = r[CATEGORIZATION_RULES.CREATED_AT]!!,
        updatedAt = r[CATEGORIZATION_RULES.MODIFIED_AT]!!,
    )
}
