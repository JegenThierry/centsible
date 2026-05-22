package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.transaction.AttachmentEnrichedDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.transactions.IAttachmentService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/attachments")
@RestController
class UserAttachmentsResource(private val service: IAttachmentService) {

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<List<AttachmentEnrichedDTO>> =
        ResponseEntity.ok(service.listForUser(user, page, size))
}
