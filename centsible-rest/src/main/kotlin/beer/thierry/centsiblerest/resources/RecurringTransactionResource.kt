package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.recurring.RecurringTransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.recurring.IRecurringTransactionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RequestMapping("/api/recurring-transactions")
@RestController
class RecurringTransactionResource(private val service: IRecurringTransactionService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) accountId: UUID?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<RecurringTransactionDTO>> =
        ResponseEntity.ok(service.fetchAll(authenticatedUser, accountId))

    @PostMapping("/{accountId}")
    fun create(
        @PathVariable accountId: UUID,
        @Valid @RequestBody form: RecurringTransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> =
        ResponseEntity.ok(service.create(accountId, form, authenticatedUser))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: RecurringTransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> =
        ResponseEntity.ok(service.update(id, form, authenticatedUser))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        service.delete(id, authenticatedUser)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/pause")
    fun pause(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> =
        ResponseEntity.ok(service.setActive(id, false, authenticatedUser))

    @PostMapping("/{id}/resume")
    fun resume(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> =
        ResponseEntity.ok(service.setActive(id, true, authenticatedUser))
}
