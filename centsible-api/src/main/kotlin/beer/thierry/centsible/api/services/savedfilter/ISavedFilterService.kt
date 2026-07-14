package beer.thierry.centsible.api.services.savedfilter

import beer.thierry.centsible.api.model.savedfilter.SavedFilterDTO
import beer.thierry.centsible.api.model.savedfilter.SavedFilterForm
import beer.thierry.centsible.api.model.user.UserDTO

/** CRUD for saved transaction filter views. Owner-scoped (ADR-0003); names unique per user. */
interface ISavedFilterService {
    fun fetchAll(authenticatedUser: UserDTO): List<SavedFilterDTO>
    fun create(authenticatedUser: UserDTO, form: SavedFilterForm): SavedFilterDTO
    fun update(authenticatedUser: UserDTO, id: Long, form: SavedFilterForm): SavedFilterDTO
    fun delete(authenticatedUser: UserDTO, id: Long)
}
