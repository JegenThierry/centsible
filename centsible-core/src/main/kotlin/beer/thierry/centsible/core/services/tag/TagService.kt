package beer.thierry.centsible.core.services.tag

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.tag.TagDTO
import beer.thierry.centsible.api.model.tag.TagForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ITagRepository
import beer.thierry.centsible.api.services.tag.ITagService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TagService(
    private val repository: ITagRepository,
) : ITagService {

    private val log = LoggerFactory.getLogger(TagService::class.java)

    override fun fetchAll(authenticatedUser: UserDTO): List<TagDTO> =
        repository.fetchAll(authenticatedUser)

    override fun create(authenticatedUser: UserDTO, form: TagForm): TagDTO {
        val name = form.name.trim()
        if (repository.existsByName(authenticatedUser, name)) {
            throw LocalizedException.Conflict("error.tag.nameTaken", name)
        }
        val created = repository.create(authenticatedUser, form.copy(name = name))
        log.info("Created tag id={} userId={}", created.id, authenticatedUser.id)
        return created
    }

    override fun update(authenticatedUser: UserDTO, id: Long, form: TagForm): TagDTO {
        repository.fetchById(authenticatedUser, id)
            ?: throw LocalizedException.NotFound("error.tag.notFound")
        val name = form.name.trim()
        if (repository.existsByName(authenticatedUser, name, excludeId = id)) {
            throw LocalizedException.Conflict("error.tag.nameTaken", name)
        }
        val updated = repository.update(authenticatedUser, id, form.copy(name = name))
            ?: throw LocalizedException.NotFound("error.tag.notFound")
        log.info("Updated tag id={} userId={}", id, authenticatedUser.id)
        return updated
    }

    override fun delete(authenticatedUser: UserDTO, id: Long) {
        if (!repository.delete(authenticatedUser, id)) {
            throw LocalizedException.NotFound("error.tag.notFound")
        }
        log.info("Deleted tag id={} userId={}", id, authenticatedUser.id)
    }

    override fun fetchTagsForTransaction(authenticatedUser: UserDTO, transactionId: UUID): List<TagDTO> =
        repository.fetchTagsForTransaction(authenticatedUser, transactionId)

    @Transactional
    override fun setTransactionTags(
        authenticatedUser: UserDTO,
        transactionId: UUID,
        tagIds: List<Long>,
    ): List<TagDTO> {
        val distinct = tagIds.distinct()
        if (distinct.isNotEmpty()) {
            val owned = repository.fetchAll(authenticatedUser).mapNotNull { it.id }.toSet()
            if (distinct.any { it !in owned }) {
                throw LocalizedException.BadRequest("error.tag.notAccessible")
            }
        }
        if (!repository.setTransactionTags(authenticatedUser, transactionId, distinct)) {
            throw LocalizedException.NotFound("error.transaction.notFound")
        }
        log.info("Set {} tag(s) on transaction id={} userId={}", distinct.size, transactionId, authenticatedUser.id)
        return repository.fetchTagsForTransaction(authenticatedUser, transactionId)
    }
}
