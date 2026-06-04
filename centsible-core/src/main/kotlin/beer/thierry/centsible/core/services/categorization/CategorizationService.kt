package beer.thierry.centsible.core.services.categorization

import beer.thierry.centsible.api.model.categorization.CategorizationRuleDTO
import beer.thierry.centsible.api.model.categorization.CategorizationRuleForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.ICategorizationRuleRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.services.categorization.ICategorizationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategorizationService(
    private val rules: ICategorizationRuleRepository,
    private val categories: ICategoriesRepository,
    private val transactions: ITransactionRepository,
) : ICategorizationService {

    private val log = LoggerFactory.getLogger(CategorizationService::class.java)

    override fun list(user: UserDTO): List<CategorizationRuleDTO> = rules.fetchAll(user)

    @Transactional
    override fun create(user: UserDTO, form: CategorizationRuleForm): CategorizationRuleDTO {
        requireUsableCategory(user, form.categoryId)
        return rules.create(user, form.copy(pattern = form.pattern.trim()))
    }

    @Transactional
    override fun update(user: UserDTO, id: UUID, form: CategorizationRuleForm): CategorizationRuleDTO {
        requireUsableCategory(user, form.categoryId)
        return rules.update(user, id, form.copy(pattern = form.pattern.trim()))
            ?: throw IllegalArgumentException("Categorization rule $id not found")
    }

    override fun delete(user: UserDTO, id: UUID): Boolean = rules.delete(user, id)

    @Transactional
    override fun applyToExisting(user: UserDTO, ruleId: UUID): Int {
        val rule = rules.fetchById(user, ruleId)
            ?: throw IllegalArgumentException("Categorization rule $ruleId not found")
        val categoryId = rule.category.id ?: return 0
        val type = rule.category.type ?: return 0
        val updated = transactions.recategorizeByDescription(user, rule.matchType, rule.pattern, categoryId, type)
        log.info("Applied rule {} to {} existing transaction(s) userId={}", ruleId, updated, user.id)
        return updated
    }

    private fun requireUsableCategory(user: UserDTO, categoryId: Long) {
        categories.fetchCategoryById(user, categoryId)
            ?: throw IllegalArgumentException("Category $categoryId not found or not usable for a rule")
    }
}
