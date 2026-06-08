package beer.thierry.centsible.core.services.rule

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.rule.RuleActionForm
import beer.thierry.centsible.api.model.rule.RuleActionType
import beer.thierry.centsible.api.model.rule.RuleConditionForm
import beer.thierry.centsible.api.model.rule.RuleContext
import beer.thierry.centsible.api.model.rule.RuleDTO
import beer.thierry.centsible.api.model.rule.RuleField
import beer.thierry.centsible.api.model.rule.RuleForm
import beer.thierry.centsible.api.model.rule.RuleMatching
import beer.thierry.centsible.api.model.rule.RuleOperator
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.IRuleRepository
import beer.thierry.centsible.core.services.categories.requireOwnedClassification
import beer.thierry.centsible.api.repository.ITagRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.services.rule.IRuleService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RuleService(
    private val rules: IRuleRepository,
    private val categories: ICategoriesRepository,
    private val tags: ITagRepository,
    private val transactions: ITransactionRepository,
) : IRuleService {

    private val log = LoggerFactory.getLogger(RuleService::class.java)

    override fun list(user: UserDTO): List<RuleDTO> = rules.fetchAll(user)

    @Transactional
    override fun create(user: UserDTO, form: RuleForm): RuleDTO =
        rules.create(user, validateAndNormalize(user, form))

    @Transactional
    override fun update(user: UserDTO, id: UUID, form: RuleForm): RuleDTO =
        rules.update(user, id, validateAndNormalize(user, form))
            ?: throw LocalizedException.NotFound("error.rule.notFound")

    override fun delete(user: UserDTO, id: UUID): Boolean = rules.delete(user, id)

    @Transactional
    override fun applyToExisting(user: UserDTO, ruleId: UUID): Int {
        val rule = rules.fetchById(user, ruleId)
            ?: throw LocalizedException.NotFound("error.rule.notFound")

        val matched = transactions.fetchForRuleEvaluation(user).filter { c ->
            RuleMatching.ruleMatches(rule, RuleContext(c.description, c.amount, c.type, c.accountId))
        }
        if (matched.isEmpty()) return 0

        val touched = HashSet<UUID>()

        val category = rule.actions.firstOrNull { it.type == RuleActionType.SET_CATEGORY && it.category?.id != null }?.category
        if (category?.id != null) {
            val targets = matched
                .filter { !it.isManagedCategory && it.type == category.type && it.categoryId != category.id }
                .map { it.id }
            if (targets.isNotEmpty()) {
                transactions.setCategoryForTransactions(user, targets, category.id!!)
                touched.addAll(targets)
            }
        }

        val tagIds = rule.actions.filter { it.type == RuleActionType.ADD_TAG }.mapNotNull { it.tag?.id }.distinct()
        if (tagIds.isNotEmpty()) {
            val txIds = matched.map { it.id }
            tags.addTagsToTransactions(user, txIds, tagIds)
            touched.addAll(txIds)
        }

        log.info("Applied rule {} to {} existing transaction(s) userId={}", ruleId, touched.size, user.id)
        return touched.size
    }

    private fun validateAndNormalize(user: UserDTO, form: RuleForm): RuleForm {
        if (form.conditions.isEmpty()) throw LocalizedException.BadRequest("error.rule.conditionsRequired")
        if (form.actions.isEmpty()) throw LocalizedException.BadRequest("error.rule.actionsRequired")
        return form.copy(
            name = form.name.trim(),
            conditions = form.conditions.map { validateCondition(it) },
            actions = form.actions.map { validateAction(user, it) },
        )
    }

    private fun validateCondition(condition: RuleConditionForm): RuleConditionForm {
        val value = condition.value.trim()
        val operatorOk = when (condition.field) {
            RuleField.DESCRIPTION -> condition.operator in DESCRIPTION_OPERATORS
            RuleField.AMOUNT -> condition.operator in AMOUNT_OPERATORS
            RuleField.DIRECTION, RuleField.ACCOUNT -> condition.operator == RuleOperator.IS
        }
        if (!operatorOk) throw LocalizedException.BadRequest("error.rule.conditionInvalid")

        val valueOk = when (condition.field) {
            RuleField.DESCRIPTION -> value.isNotBlank()
            RuleField.AMOUNT -> value.toBigDecimalOrNull() != null
            RuleField.DIRECTION -> runCatching { CategoryType.valueOf(value.uppercase()) }.isSuccess
            RuleField.ACCOUNT -> runCatching { UUID.fromString(value) }.isSuccess
        }
        if (!valueOk) throw LocalizedException.BadRequest("error.rule.conditionInvalid")

        val canonical = if (condition.field == RuleField.DIRECTION) value.uppercase() else value
        return condition.copy(value = canonical)
    }

    private fun validateAction(user: UserDTO, action: RuleActionForm): RuleActionForm = when (action.type) {
        RuleActionType.SET_CATEGORY -> {
            val categoryId = action.categoryId ?: throw LocalizedException.BadRequest("error.rule.actionInvalid")
            val classification = categories.requireOwnedClassification(user, categoryId)
            if (classification.isManaged) throw LocalizedException.BadRequest("error.category.managedAssign")
            action.copy(categoryId = categoryId, tagId = null)
        }

        RuleActionType.ADD_TAG -> {
            val tagId = action.tagId ?: throw LocalizedException.BadRequest("error.rule.actionInvalid")
            tags.fetchById(user, tagId) ?: throw LocalizedException.BadRequest("error.tag.notAccessible")
            action.copy(tagId = tagId, categoryId = null)
        }
    }

    companion object {
        private val DESCRIPTION_OPERATORS = setOf(RuleOperator.CONTAINS, RuleOperator.EQUALS, RuleOperator.STARTS_WITH)
        private val AMOUNT_OPERATORS =
            setOf(RuleOperator.GT, RuleOperator.GTE, RuleOperator.LT, RuleOperator.LTE, RuleOperator.EQUALS)
    }
}
