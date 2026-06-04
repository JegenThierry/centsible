package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.categorization.CategorizationRuleDTO
import beer.thierry.centsible.api.model.categorization.CategorizationRuleForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

interface ICategorizationRuleRepository {
    fun fetchAll(user: UserDTO): List<CategorizationRuleDTO>
    fun fetchById(user: UserDTO, id: UUID): CategorizationRuleDTO?
    fun create(user: UserDTO, form: CategorizationRuleForm): CategorizationRuleDTO
    fun update(user: UserDTO, id: UUID, form: CategorizationRuleForm): CategorizationRuleDTO?
    fun delete(user: UserDTO, id: UUID): Boolean
}
