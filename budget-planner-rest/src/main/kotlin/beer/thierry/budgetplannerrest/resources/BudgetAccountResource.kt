package beer.thierry.budgetplannerrest.resources

import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.services.account.IBudgetAccountService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RequestMapping("/api/budget-accounts")
@RestController
class BudgetAccountResource(private val budgetAccountService: IBudgetAccountService) {

    @PostMapping("")
    fun createAccount(
        @Valid @RequestBody createBudgetAccountRequest: CreateBudgetAccountRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<BudgetAccountDTO> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val response = budgetAccountService.createAccount(createBudgetAccountRequest, authenticatedUser)
        return ResponseEntity.ok(response)
    }

    @GetMapping("")
    fun fetchAccounts(@AuthenticationPrincipal authenticatedUser: UserDTO?): ResponseEntity<List<BudgetAccountDTO>> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val response = budgetAccountService.fetchAccounts(authenticatedUser)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun findAccount(
        @PathVariable id: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<BudgetAccountDTO> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val response = budgetAccountService.fetchAccountById(id, authenticatedUser)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/snapshots")
    fun fetchSnapshots(
        @PathVariable id: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<List<BudgetAccountSnapshotDTO>> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val response = budgetAccountService.fetchAccountSnapshots(id, startDate, endDate, authenticatedUser)
        return ResponseEntity.ok(response)
    }
}
