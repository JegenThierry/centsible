package beer.thierry.budgetplannerrest.controller

import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.service.account.IBudgetAccountService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RequestMapping("/api/budget-accounts")
@RestController
class BudgetAccountController(private val budgetAccountService: IBudgetAccountService) {

    @PostMapping("")
    fun createAccount(
        @RequestBody createBudgetAccountRequest: CreateBudgetAccountRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO
    ): ResponseEntity<BudgetAccountDTO> {
        val response = budgetAccountService.createAccount(createBudgetAccountRequest, authenticatedUser)
        return ResponseEntity.ok(response)
    }

    @GetMapping("")
    fun fetchAccounts(@AuthenticationPrincipal authenticatedUser: UserDTO): ResponseEntity<List<BudgetAccountDTO>> {
        val response = budgetAccountService.fetchAccounts(authenticatedUser)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun findAccount(
        @PathVariable id: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO
    ): ResponseEntity<BudgetAccountDTO> {
        val response = budgetAccountService.fetchAccountById(id, authenticatedUser)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/snapshots")
    fun fetchSnapshots(
        @PathVariable id: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO
    ): ResponseEntity<List<BudgetAccountSnapshotDTO>> {
        val response = budgetAccountService.fetchAccountSnapshots(id, startDate, endDate, authenticatedUser)
        return ResponseEntity.ok(response)
    }
}
