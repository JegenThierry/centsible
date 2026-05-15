package beer.thierry.budgetplanner.api.services.integrations

import beer.thierry.budgetplanner.api.model.integrations.ProviderConnectionDTO
import beer.thierry.budgetplanner.api.model.integrations.ProviderConnectionForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.util.UUID

interface IProviderConnectionService {
    fun fetchAllConnections(authenticatedUser: UserDTO): List<ProviderConnectionDTO>
    fun fetchConnectionById(authenticatedUser: UserDTO, id: UUID): ProviderConnectionDTO?
    fun createConnection(authenticatedUser: UserDTO, form: ProviderConnectionForm): ProviderConnectionDTO
    fun updateConnection(authenticatedUser: UserDTO, id: UUID, form: ProviderConnectionForm): ProviderConnectionDTO?
    fun deleteConnection(authenticatedUser: UserDTO, id: UUID): Boolean
    fun triggerSync(authenticatedUser: UserDTO, id: UUID): Boolean
}
