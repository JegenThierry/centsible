package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.budget.BudgetSuggestionDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.budget.IBudgetService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.YearMonth
import java.util.UUID

@RequestMapping("/api/budgets")
@RestController
class BudgetsResource(private val service: IBudgetService) {

    private val log = LoggerFactory.getLogger(BudgetsResource::class.java)

    @GetMapping
    fun list(
        @RequestParam(required = false) month: YearMonth?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<BudgetDTO> =
        service.fetchAllForMonth(authenticatedUser, month ?: YearMonth.now())

    @GetMapping("/suggestions")
    fun suggestions(
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<BudgetSuggestionDTO> =
        service.suggestions(authenticatedUser)

    @PostMapping
    fun create(
        @Valid @RequestBody form: BudgetForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): BudgetDTO {
        val created = service.create(form, authenticatedUser)
        log.info("Created budget id={} userId={}", created.id, authenticatedUser.id)
        return created
    }

    @PostMapping("/bulk-suggested")
    fun bulkCreateSuggested(
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<BudgetDTO> {
        val created = service.bulkCreateSuggested(authenticatedUser)
        log.info("Bulk-created {} budget(s) from suggestions userId={}", created.size, authenticatedUser.id)
        return created
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: BudgetForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): BudgetDTO {
        val updated = service.update(id, form, authenticatedUser)
        log.info("Updated budget id={} userId={}", id, authenticatedUser.id)
        return updated
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        service.delete(id, authenticatedUser)
        log.info("Deleted budget id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }
}
