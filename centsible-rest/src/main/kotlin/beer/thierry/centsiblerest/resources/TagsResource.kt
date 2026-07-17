package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.tag.TagDTO
import beer.thierry.centsible.api.model.tag.TagForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.tag.ITagService
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

/** CRUD for user tags. Authorization is enforced in the service layer (ADR-0003). */
@RequestMapping("/api/tags")
@RestController
class TagsResource(
    private val service: ITagService,
) {

    @GetMapping
    fun list(@AuthenticationPrincipal user: UserDTO): List<TagDTO> =
        service.fetchAll(user)

    @PostMapping
    fun create(
        @Valid @RequestBody form: TagForm,
        @AuthenticationPrincipal user: UserDTO,
    ): TagDTO =
        service.create(user, form)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody form: TagForm,
        @AuthenticationPrincipal user: UserDTO,
    ): TagDTO =
        service.update(user, id, form)

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<Void> {
        service.delete(user, id)
        return ResponseEntity.noContent().build()
    }
}
