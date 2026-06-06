package beer.thierry.centsible.api.services.categorization

import beer.thierry.centsible.api.model.categorization.CategorizationRuleDTO
import beer.thierry.centsible.api.model.categorization.CategorizationRuleForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

interface ICategorizationService {
    fun list(user: UserDTO): List<CategorizationRuleDTO>
    fun create(user: UserDTO, form: CategorizationRuleForm): CategorizationRuleDTO
    fun update(user: UserDTO, id: UUID, form: CategorizationRuleForm): CategorizationRuleDTO
    fun delete(user: UserDTO, id: UUID): Boolean
    fun applyToExisting(user: UserDTO, ruleId: UUID): Int
}
