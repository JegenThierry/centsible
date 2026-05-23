package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.recurring.RecurringTransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.recurring.IRecurringTransactionService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RequestMapping("/api/recurring-transactions")
@RestController
class RecurringTransactionResource(private val service: IRecurringTransactionService) {

    private val log = LoggerFactory.getLogger(RecurringTransactionResource::class.java)

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
    ): ResponseEntity<RecurringTransactionDTO> {
        val created = service.create(accountId, form, authenticatedUser)
        log.info("Created recurring transaction id={} accountId={} userId={}", created.id, accountId, authenticatedUser.id)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: RecurringTransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> {
        val updated = service.update(id, form, authenticatedUser)
        log.info("Updated recurring transaction id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        service.delete(id, authenticatedUser)
        log.info("Deleted recurring transaction id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/pause")
    fun pause(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> {
        val result = service.setActive(id, false, authenticatedUser)
        log.info("Paused recurring transaction id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{id}/resume")
    fun resume(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RecurringTransactionDTO> {
        val result = service.setActive(id, true, authenticatedUser)
        log.info("Resumed recurring transaction id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.ok(result)
    }
}
