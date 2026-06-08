package beer.thierry.centsible.core.services.imports

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IImportMappingTemplateRepository
import beer.thierry.centsible.api.services.imports.IImportMappingTemplateService
import beer.thierry.centsible.api.services.imports.ImportMappingTemplateDTO
import beer.thierry.centsible.api.services.imports.ImportMappingTemplateForm
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ImportMappingTemplateService(
    private val repository: IImportMappingTemplateRepository,
) : IImportMappingTemplateService {

    private val log = LoggerFactory.getLogger(ImportMappingTemplateService::class.java)

    override fun fetchAll(authenticatedUser: UserDTO): List<ImportMappingTemplateDTO> =
        repository.fetchAll(authenticatedUser)

    override fun create(authenticatedUser: UserDTO, form: ImportMappingTemplateForm): ImportMappingTemplateDTO {
        val name = form.name.trim()
        if (repository.existsByName(authenticatedUser, name)) {
            throw LocalizedException.Conflict("error.importTemplate.nameTaken", name)
        }
        val created = repository.create(authenticatedUser, form)
        log.info("Created import mapping template id={} userId={}", created.id, authenticatedUser.id)
        return created
    }

    override fun update(
        authenticatedUser: UserDTO,
        id: UUID,
        form: ImportMappingTemplateForm,
    ): ImportMappingTemplateDTO {
        repository.fetchById(authenticatedUser, id)
            ?: throw LocalizedException.NotFound("error.importTemplate.notFound")
        val name = form.name.trim()
        if (repository.existsByName(authenticatedUser, name, excludeId = id)) {
            throw LocalizedException.Conflict("error.importTemplate.nameTaken", name)
        }
        val updated = repository.update(authenticatedUser, id, form)
            ?: throw LocalizedException.NotFound("error.importTemplate.notFound")
        log.info("Updated import mapping template id={} userId={}", id, authenticatedUser.id)
        return updated
    }

    override fun delete(authenticatedUser: UserDTO, id: UUID) {
        val deleted = repository.delete(authenticatedUser, id)
        if (!deleted) {
            throw LocalizedException.NotFound("error.importTemplate.notFound")
        }
        log.info("Deleted import mapping template id={} userId={}", id, authenticatedUser.id)
    }
}
