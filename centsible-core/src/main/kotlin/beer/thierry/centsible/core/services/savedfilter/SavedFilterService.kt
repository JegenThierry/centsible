package beer.thierry.centsible.core.services.savedfilter

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.savedfilter.SavedFilterDTO
import beer.thierry.centsible.api.model.savedfilter.SavedFilterForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ISavedFilterRepository
import beer.thierry.centsible.api.services.savedfilter.ISavedFilterService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SavedFilterService(
    private val repository: ISavedFilterRepository,
) : ISavedFilterService {

    private val log = LoggerFactory.getLogger(SavedFilterService::class.java)

    override fun fetchAll(authenticatedUser: UserDTO): List<SavedFilterDTO> =
        repository.fetchAll(authenticatedUser)

    override fun create(authenticatedUser: UserDTO, form: SavedFilterForm): SavedFilterDTO {
        val name = form.name.trim()
        if (repository.existsByName(authenticatedUser, name)) {
            throw LocalizedException.Conflict("error.savedFilter.nameTaken", name)
        }
        val created = repository.create(authenticatedUser, form.copy(name = name))
        log.info("Created saved filter id={} userId={}", created.id, authenticatedUser.id)
        return created
    }

    override fun update(authenticatedUser: UserDTO, id: Long, form: SavedFilterForm): SavedFilterDTO {
        repository.fetchById(authenticatedUser, id)
            ?: throw LocalizedException.NotFound("error.savedFilter.notFound")
        val name = form.name.trim()
        if (repository.existsByName(authenticatedUser, name, excludeId = id)) {
            throw LocalizedException.Conflict("error.savedFilter.nameTaken", name)
        }
        val updated = repository.update(authenticatedUser, id, form.copy(name = name))
            ?: throw LocalizedException.NotFound("error.savedFilter.notFound")
        log.info("Updated saved filter id={} userId={}", id, authenticatedUser.id)
        return updated
    }

    override fun delete(authenticatedUser: UserDTO, id: Long) {
        if (!repository.delete(authenticatedUser, id)) {
            throw LocalizedException.NotFound("error.savedFilter.notFound")
        }
        log.info("Deleted saved filter id={} userId={}", id, authenticatedUser.id)
    }
}
