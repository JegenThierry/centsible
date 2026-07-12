package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.imports.ImportMappingTemplateDTO
import beer.thierry.centsible.api.services.imports.ImportMappingTemplateForm
import java.util.UUID

/** Persistence for user-saved CSV import profiles. All reads/writes are scoped to the owner (ADR-0003). */
interface IImportMappingTemplateRepository {
    fun fetchAll(authenticatedUser: UserDTO): List<ImportMappingTemplateDTO>
    fun fetchById(authenticatedUser: UserDTO, id: UUID): ImportMappingTemplateDTO?
    fun existsByName(authenticatedUser: UserDTO, name: String, excludeId: UUID? = null): Boolean
    fun create(authenticatedUser: UserDTO, form: ImportMappingTemplateForm): ImportMappingTemplateDTO
    fun update(authenticatedUser: UserDTO, id: UUID, form: ImportMappingTemplateForm): ImportMappingTemplateDTO?
    fun delete(authenticatedUser: UserDTO, id: UUID): Boolean
}
