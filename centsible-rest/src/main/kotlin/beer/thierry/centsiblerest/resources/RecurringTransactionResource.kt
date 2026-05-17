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
        @RequestParam(required = false) accountId: String?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<RecurringTransactionDTO>> =
        ResponseEntity.ok(service.fetchAll(authenticatedUser, accountId?.let(UUID::fromString)))

    @PostMapping("/{accountId}")
    fun create(
        @PathVariable accountId: String,
        @Valid @RequestBody form: RecurringTransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> =
        ResponseEntity.ok(service.create(UUID.fromString(accountId), form, authenticatedUser))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody form: RecurringTransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> =
        ResponseEntity.ok(service.update(UUID.fromString(id), form, authenticatedUser))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        service.delete(UUID.fromString(id), authenticatedUser)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/pause")
    fun pause(
        @PathVariable id: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> =
        ResponseEntity.ok(service.setActive(UUID.fromString(id), false, authenticatedUser))

    @PostMapping("/{id}/resume")
    fun resume(
        @PathVariable id: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> =
        ResponseEntity.ok(service.setActive(UUID.fromString(id), true, authenticatedUser))
}
