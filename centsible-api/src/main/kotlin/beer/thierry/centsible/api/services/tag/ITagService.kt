package beer.thierry.centsible.api.services.tag

import beer.thierry.centsible.api.model.tag.TagDTO
import beer.thierry.centsible.api.model.tag.TagForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

/** CRUD for user tags plus per-transaction assignment. Owner-scoped (ADR-0003); names unique per user. */
interface ITagService {
    fun fetchAll(authenticatedUser: UserDTO): List<TagDTO>
    fun create(authenticatedUser: UserDTO, form: TagForm): TagDTO
    fun update(authenticatedUser: UserDTO, id: Long, form: TagForm): TagDTO
    fun delete(authenticatedUser: UserDTO, id: Long)
    fun fetchTagsForTransaction(authenticatedUser: UserDTO, transactionId: UUID): List<TagDTO>
    fun setTransactionTags(authenticatedUser: UserDTO, transactionId: UUID, tagIds: List<Long>): List<TagDTO>
}
