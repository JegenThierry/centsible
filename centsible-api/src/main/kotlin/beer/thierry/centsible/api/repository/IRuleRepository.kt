package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.rule.RuleDTO
import beer.thierry.centsible.api.model.rule.RuleForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

/** Persistence for user-scoped rules with their conditions and actions. Owner-scoped (ADR-0003). */
interface IRuleRepository {
    fun fetchAll(user: UserDTO): List<RuleDTO>
    fun fetchById(user: UserDTO, id: UUID): RuleDTO?
    fun create(user: UserDTO, form: RuleForm): RuleDTO
    fun update(user: UserDTO, id: UUID, form: RuleForm): RuleDTO?
    fun delete(user: UserDTO, id: UUID): Boolean
}
