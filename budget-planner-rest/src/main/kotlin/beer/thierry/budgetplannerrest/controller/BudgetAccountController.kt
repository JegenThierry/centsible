package beer.thierry.budgetplannerrest.controller

import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.service.account.IBudgetAccountService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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

}