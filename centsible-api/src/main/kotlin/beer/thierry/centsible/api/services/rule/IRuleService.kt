package beer.thierry.centsible.api.services.rule

import beer.thierry.centsible.api.model.rule.RuleDTO
import beer.thierry.centsible.api.model.rule.RuleForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

interface IRuleService {
    fun list(user: UserDTO): List<RuleDTO>
    fun create(user: UserDTO, form: RuleForm): RuleDTO
    fun update(user: UserDTO, id: UUID, form: RuleForm): RuleDTO
    fun delete(user: UserDTO, id: UUID): Boolean

    /** Re-evaluates [ruleId] against the user's existing transactions, applying its actions. Returns rows touched. */
    fun applyToExisting(user: UserDTO, ruleId: UUID): Int
}
