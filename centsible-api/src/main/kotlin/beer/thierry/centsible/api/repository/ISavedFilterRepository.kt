package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.savedfilter.SavedFilterDTO
import beer.thierry.centsible.api.model.savedfilter.SavedFilterForm
import beer.thierry.centsible.api.model.user.UserDTO

/** Persistence for user-scoped saved transaction filter views. All access is owner-scoped (ADR-0003). */
interface ISavedFilterRepository {
    fun fetchAll(authenticatedUser: UserDTO): List<SavedFilterDTO>
    fun fetchById(authenticatedUser: UserDTO, id: Long): SavedFilterDTO?
    fun existsByName(authenticatedUser: UserDTO, name: String, excludeId: Long? = null): Boolean
    fun create(authenticatedUser: UserDTO, form: SavedFilterForm): SavedFilterDTO
    fun update(authenticatedUser: UserDTO, id: Long, form: SavedFilterForm): SavedFilterDTO?
    fun delete(authenticatedUser: UserDTO, id: Long): Boolean
}
