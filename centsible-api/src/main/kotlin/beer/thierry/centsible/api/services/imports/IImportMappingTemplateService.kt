package beer.thierry.centsible.api.services.imports

import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

/**
 * CRUD for user-saved CSV import profiles ([ImportMappingTemplateDTO]). Every operation is scoped to
 * the authenticated user (ADR-0003); profile names are unique per user, case-insensitively.
 */
interface IImportMappingTemplateService {
    fun fetchAll(authenticatedUser: UserDTO): List<ImportMappingTemplateDTO>
    fun create(authenticatedUser: UserDTO, form: ImportMappingTemplateForm): ImportMappingTemplateDTO
    fun update(authenticatedUser: UserDTO, id: UUID, form: ImportMappingTemplateForm): ImportMappingTemplateDTO
    fun delete(authenticatedUser: UserDTO, id: UUID)
}
