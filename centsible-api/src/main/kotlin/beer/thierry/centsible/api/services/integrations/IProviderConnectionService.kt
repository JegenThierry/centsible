package beer.thierry.centsible.api.services.integrations

import beer.thierry.centsible.api.model.integrations.ProviderConnectionDTO
import beer.thierry.centsible.api.model.integrations.ProviderConnectionForm
import beer.thierry.centsible.api.model.integrations.SelectOption
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

interface IProviderConnectionService {
    fun fetchAllConnections(authenticatedUser: UserDTO): List<ProviderConnectionDTO>
    fun fetchConnectionById(authenticatedUser: UserDTO, id: UUID): ProviderConnectionDTO?
    fun createConnection(authenticatedUser: UserDTO, form: ProviderConnectionForm): ProviderConnectionDTO
    fun updateConnection(authenticatedUser: UserDTO, id: UUID, form: ProviderConnectionForm): ProviderConnectionDTO?
    fun deleteConnection(authenticatedUser: UserDTO, id: UUID): Boolean
    fun triggerSync(authenticatedUser: UserDTO, id: UUID): Boolean

    /**
     * Resolves dropdown options for a SELECT_REMOTE field. Throws if the provider does not
     * implement IRemoteOptionsProvider or the field name is unknown.
     */
    fun fetchRemoteOptions(
        authenticatedUser: UserDTO,
        providerKey: String,
        fieldName: String,
        query: String?,
        values: Map<String, Any?>,
    ): List<SelectOption>
}
