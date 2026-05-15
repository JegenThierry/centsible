package beer.thierry.budgetplannerrest.resources

import beer.thierry.budgetplanner.api.model.budget.BudgetDTO
import beer.thierry.budgetplanner.api.model.budget.BudgetForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.services.budget.IBudgetService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.*

@RequestMapping("/api/budgets")
@RestController
class BudgetsResource(private val service: IBudgetService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) month: String?,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<List<BudgetDTO>> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        val yearMonth = month?.let {
            try {
                YearMonth.parse(it)
            } catch (e: DateTimeParseException) {
                return ResponseEntity.badRequest().build()
            }
        } ?: YearMonth.now()
        return ResponseEntity.ok(service.fetchAllForMonth(authenticatedUser, yearMonth))
    }

    @PostMapping
    fun create(
        @Valid @RequestBody form: BudgetForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<BudgetDTO> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        return ResponseEntity.ok(service.create(form, authenticatedUser))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody form: BudgetForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<BudgetDTO> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        return ResponseEntity.ok(service.update(UUID.fromString(id), form, authenticatedUser))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<Void> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        service.delete(UUID.fromString(id), authenticatedUser)
        return ResponseEntity.noContent().build()
    }
}
