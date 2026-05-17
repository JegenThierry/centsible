package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.budget.IBudgetService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.YearMonth
import java.util.UUID

@RequestMapping("/api/budgets")
@RestController
class BudgetsResource(private val service: IBudgetService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) month: YearMonth?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<BudgetDTO>> =
        ResponseEntity.ok(service.fetchAllForMonth(authenticatedUser, month ?: YearMonth.now()))

    @PostMapping
    fun create(
        @Valid @RequestBody form: BudgetForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<BudgetDTO> =
        ResponseEntity.ok(service.create(form, authenticatedUser))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: BudgetForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<BudgetDTO> =
        ResponseEntity.ok(service.update(id, form, authenticatedUser))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        service.delete(id, authenticatedUser)
        return ResponseEntity.noContent().build()
    }
}
