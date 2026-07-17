package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.imports.IImportMappingTemplateService
import beer.thierry.centsible.api.services.imports.ImportMappingTemplateDTO
import beer.thierry.centsible.api.services.imports.ImportMappingTemplateForm
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/imports/templates")
@RestController
class ImportMappingTemplatesResource(
    private val service: IImportMappingTemplateService,
) {

    @GetMapping
    fun list(@AuthenticationPrincipal user: UserDTO): ResponseEntity<List<ImportMappingTemplateDTO>> =
        ResponseEntity.ok(service.fetchAll(user))

    @PostMapping
    fun create(
        @Valid @RequestBody form: ImportMappingTemplateForm,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<ImportMappingTemplateDTO> =
        ResponseEntity.ok(service.create(user, form))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: ImportMappingTemplateForm,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<ImportMappingTemplateDTO> =
        ResponseEntity.ok(service.update(user, id, form))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<Void> {
        service.delete(user, id)
        return ResponseEntity.noContent().build()
    }
}
