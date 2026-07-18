package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.tag.TagDTO
import beer.thierry.centsible.api.model.tag.TransactionTagsForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.tag.ITagService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/transactions/{transactionId}/tags")
@RestController
class TransactionTagsResource(
    private val service: ITagService,
) {

    @GetMapping
    fun list(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal user: UserDTO,
    ): List<TagDTO> =
        service.fetchTagsForTransaction(user, transactionId)

    @PutMapping
    fun set(
        @PathVariable transactionId: UUID,
        @RequestBody form: TransactionTagsForm,
        @AuthenticationPrincipal user: UserDTO,
    ): List<TagDTO> =
        service.setTransactionTags(user, transactionId, form.tagIds)
}
