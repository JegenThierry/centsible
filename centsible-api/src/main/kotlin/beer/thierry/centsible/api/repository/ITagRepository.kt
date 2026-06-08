package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.tag.TagDTO
import beer.thierry.centsible.api.model.tag.TagForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

/** Persistence for user-scoped tags and their links to transactions. All access is owner-scoped (ADR-0003). */
interface ITagRepository {
    fun fetchAll(authenticatedUser: UserDTO): List<TagDTO>
    fun fetchById(authenticatedUser: UserDTO, id: Long): TagDTO?
    fun existsByName(authenticatedUser: UserDTO, name: String, excludeId: Long? = null): Boolean
    fun create(authenticatedUser: UserDTO, form: TagForm): TagDTO
    fun update(authenticatedUser: UserDTO, id: Long, form: TagForm): TagDTO?
    fun delete(authenticatedUser: UserDTO, id: Long): Boolean

    /** Tags currently attached to one of the user's transactions (empty if the transaction isn't theirs). */
    fun fetchTagsForTransaction(authenticatedUser: UserDTO, transactionId: UUID): List<TagDTO>

    /** Batch variant for list enrichment: tag lists keyed by transaction id. */
    fun fetchTagsByTransactionIds(authenticatedUser: UserDTO, transactionIds: List<UUID>): Map<UUID, List<TagDTO>>

    /**
     * Replaces the transaction's tags with [tagIds]. Returns false if the transaction is not the
     * user's. [tagIds] are assumed to be already validated as the user's own tags.
     */
    fun setTransactionTags(authenticatedUser: UserDTO, transactionId: UUID, tagIds: List<Long>): Boolean

    /**
     * Adds [tagIds] to each of [transactionIds] without removing existing tags (idempotent). Only the
     * user's own transactions and tags are linked. Returns the number of new links created.
     */
    fun addTagsToTransactions(
        authenticatedUser: UserDTO,
        transactionIds: Collection<UUID>,
        tagIds: Collection<Long>,
    ): Int
}
