package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.savedfilter.SavedFilterDTO
import beer.thierry.centsible.api.model.savedfilter.SavedFilterForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.savedfilter.ISavedFilterService
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

/** CRUD for saved transaction filter views. Authorization is enforced in the service layer (ADR-0003). */
@RequestMapping("/api/saved-filters")
@RestController
class SavedFiltersResource(
    private val service: ISavedFilterService,
) {

    @GetMapping
    fun list(@AuthenticationPrincipal user: UserDTO): ResponseEntity<List<SavedFilterDTO>> =
        ResponseEntity.ok(service.fetchAll(user))

    @PostMapping
    fun create(
        @Valid @RequestBody form: SavedFilterForm,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<SavedFilterDTO> =
        ResponseEntity.ok(service.create(user, form))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody form: SavedFilterForm,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<SavedFilterDTO> =
        ResponseEntity.ok(service.update(user, id, form))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<Void> {
        service.delete(user, id)
        return ResponseEntity.noContent().build()
    }
}
