package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.rule.RuleActionDTO
import beer.thierry.centsible.api.model.rule.RuleActionType
import beer.thierry.centsible.api.model.rule.RuleConditionDTO
import beer.thierry.centsible.api.model.rule.RuleDTO
import beer.thierry.centsible.api.model.rule.RuleField
import beer.thierry.centsible.api.model.rule.RuleForm
import beer.thierry.centsible.api.model.rule.RuleOperator
import beer.thierry.centsible.api.model.tag.TagDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IRuleRepository
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.RULES
import beer.thierry.jooq.generated.tables.references.RULE_ACTIONS
import beer.thierry.jooq.generated.tables.references.RULE_CONDITIONS
import beer.thierry.jooq.generated.tables.references.TAGS
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class RuleRepository(private val dsl: DSLContext) : IRuleRepository {

    override fun fetchAll(user: UserDTO): List<RuleDTO> {
        val rules = dsl.selectFrom(RULES)
            .where(RULES.USER_ID.eq(user.id))
            .orderBy(RULES.PRIORITY.desc(), RULES.CREATED_AT.asc())
            .fetch()
        if (rules.isEmpty()) return emptyList()
        val ids = rules.map { it[RULES.ID]!! }
        val conditions = fetchConditions(ids)
        val actions = fetchActions(ids)
        return rules.map { row ->
            val ruleId = row[RULES.ID]!!
            toDTO(row, conditions[ruleId] ?: emptyList(), actions[ruleId] ?: emptyList())
        }
    }

    override fun fetchById(user: UserDTO, id: UUID): RuleDTO? {
        val rule = dsl.selectFrom(RULES)
            .where(RULES.ID.eq(id).and(RULES.USER_ID.eq(user.id)))
            .fetchOne() ?: return null
        return toDTO(rule, fetchConditions(listOf(id))[id] ?: emptyList(), fetchActions(listOf(id))[id] ?: emptyList())
    }

    override fun create(user: UserDTO, form: RuleForm): RuleDTO {
        val now = OffsetDateTime.now()
        val ruleId = dsl.insertInto(
            RULES,
            RULES.USER_ID, RULES.NAME, RULES.MATCH_ALL, RULES.ENABLED, RULES.PRIORITY, RULES.CREATED_AT, RULES.MODIFIED_AT,
        )
            .values(user.id, form.name.trim(), form.matchAll, form.enabled, form.priority, now, now)
            .returning(RULES.ID)
            .fetchOne()?.get(RULES.ID)
            ?: throw IllegalStateException("Failed to persist rule")
        insertConditions(ruleId, form)
        insertActions(ruleId, form)
        return fetchById(user, ruleId) ?: throw IllegalStateException("Persisted rule could not be re-read")
    }

    override fun update(user: UserDTO, id: UUID, form: RuleForm): RuleDTO? {
        val updated = dsl.update(RULES)
            .set(RULES.NAME, form.name.trim())
            .set(RULES.MATCH_ALL, form.matchAll)
            .set(RULES.ENABLED, form.enabled)
            .set(RULES.PRIORITY, form.priority)
            .set(RULES.MODIFIED_AT, OffsetDateTime.now())
            .where(RULES.ID.eq(id).and(RULES.USER_ID.eq(user.id)))
            .execute()
        if (updated == 0) return null
        dsl.deleteFrom(RULE_CONDITIONS).where(RULE_CONDITIONS.RULE_ID.eq(id)).execute()
        dsl.deleteFrom(RULE_ACTIONS).where(RULE_ACTIONS.RULE_ID.eq(id)).execute()
        insertConditions(id, form)
        insertActions(id, form)
        return fetchById(user, id)
    }

    override fun delete(user: UserDTO, id: UUID): Boolean =
        dsl.deleteFrom(RULES)
            .where(RULES.ID.eq(id).and(RULES.USER_ID.eq(user.id)))
            .execute() > 0

    private fun insertConditions(ruleId: UUID, form: RuleForm) {
        if (form.conditions.isEmpty()) return
        val step = dsl.insertInto(
            RULE_CONDITIONS,
            RULE_CONDITIONS.RULE_ID, RULE_CONDITIONS.FIELD, RULE_CONDITIONS.OPERATOR, RULE_CONDITIONS.VALUE,
        )
        form.conditions.forEach { step.values(ruleId, it.field.value, it.operator.value, it.value.trim()) }
        step.execute()
    }

    private fun insertActions(ruleId: UUID, form: RuleForm) {
        if (form.actions.isEmpty()) return
        val step = dsl.insertInto(
            RULE_ACTIONS,
            RULE_ACTIONS.RULE_ID, RULE_ACTIONS.ACTION_TYPE, RULE_ACTIONS.CATEGORY_ID, RULE_ACTIONS.TAG_ID,
        )
        form.actions.forEach { step.values(ruleId, it.type.value, it.categoryId, it.tagId) }
        step.execute()
    }

    private fun fetchConditions(ruleIds: List<UUID>): Map<UUID, List<RuleConditionDTO>> {
        if (ruleIds.isEmpty()) return emptyMap()
        return dsl.selectFrom(RULE_CONDITIONS)
            .where(RULE_CONDITIONS.RULE_ID.`in`(ruleIds))
            .orderBy(RULE_CONDITIONS.ID.asc())
            .fetch()
            .groupBy({ it[RULE_CONDITIONS.RULE_ID]!! }) {
                RuleConditionDTO(
                    id = it[RULE_CONDITIONS.ID],
                    field = RuleField.fromValue(it[RULE_CONDITIONS.FIELD]!!),
                    operator = RuleOperator.fromValue(it[RULE_CONDITIONS.OPERATOR]!!),
                    value = it[RULE_CONDITIONS.VALUE]!!,
                )
            }
    }

    private fun fetchActions(ruleIds: List<UUID>): Map<UUID, List<RuleActionDTO>> {
        if (ruleIds.isEmpty()) return emptyMap()
        return dsl.select(
            RULE_ACTIONS.ID, RULE_ACTIONS.RULE_ID, RULE_ACTIONS.ACTION_TYPE,
            CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.ICON, CATEGORIES.COLOR,
            CATEGORIES.TYPE, CATEGORIES.USER_ID, CATEGORIES.SYSTEM_KEY,
            TAGS.ID, TAGS.NAME, TAGS.COLOR, TAGS.CREATED_AT, TAGS.MODIFIED_AT,
        )
            .from(RULE_ACTIONS)
            .leftJoin(CATEGORIES).on(CATEGORIES.ID.eq(RULE_ACTIONS.CATEGORY_ID))
            .leftJoin(TAGS).on(TAGS.ID.eq(RULE_ACTIONS.TAG_ID))
            .where(RULE_ACTIONS.RULE_ID.`in`(ruleIds))
            .orderBy(RULE_ACTIONS.ID.asc())
            .filter { it[RULE_ACTIONS.RULE_ID] != null }
            .groupBy({ it[RULE_ACTIONS.RULE_ID]!! }, ::mapAction)
    }

    private fun mapAction(r: Record): RuleActionDTO {
        val type = RuleActionType.fromValue(r[RULE_ACTIONS.ACTION_TYPE]!!)
        return when (type) {
            RuleActionType.SET_CATEGORY -> RuleActionDTO(
                id = r[RULE_ACTIONS.ID],
                type = type,
                category = r[CATEGORIES.ID]?.let {
                    CategoryDTO(
                        id = it,
                        name = r[CATEGORIES.NAME],
                        icon = r[CATEGORIES.ICON],
                        color = r[CATEGORIES.COLOR],
                        type = r[CATEGORIES.TYPE]?.let { value -> CategoryType.fromValue(value) },
                        isSystem = r[CATEGORIES.USER_ID] == null,
                        systemKey = r[CATEGORIES.SYSTEM_KEY],
                    )
                },
            )

            RuleActionType.ADD_TAG -> RuleActionDTO(
                id = r[RULE_ACTIONS.ID],
                type = type,
                tag = r[TAGS.ID]?.let {
                    TagDTO(
                        id = it,
                        name = r[TAGS.NAME] ?: "",
                        color = r[TAGS.COLOR] ?: "#6b7280",
                        createdAt = r[TAGS.CREATED_AT],
                        modifiedAt = r[TAGS.MODIFIED_AT],
                    )
                },
            )
        }
    }

    private fun toDTO(row: Record, conditions: List<RuleConditionDTO>, actions: List<RuleActionDTO>): RuleDTO =
        RuleDTO(
            id = row[RULES.ID]!!,
            name = row[RULES.NAME]!!,
            matchAll = row[RULES.MATCH_ALL]!!,
            enabled = row[RULES.ENABLED]!!,
            priority = row[RULES.PRIORITY]!!,
            conditions = conditions,
            actions = actions,
            createdAt = row[RULES.CREATED_AT]!!,
            updatedAt = row[RULES.MODIFIED_AT]!!,
        )
}
