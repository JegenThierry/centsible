package beer.thierry.budgetplannerrest.controller

import beer.thierry.budgetplannerrest.model.transaction.TransactionForm
import beer.thierry.budgetplannerrest.model.user.UserDTO
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

@RequestMapping("/api/transactions")
@RestController
class TransactionController {
    @GetMapping("/{accountId}")
    fun fetchTransactions(
        @PathVariable accountId: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO
    ): ResponseEntity<String> {
        return ResponseEntity.ok("Transaction created")
    }

    @PostMapping("/{accountId}")
    fun createTransaction(
        @PathVariable accountId: String,
        @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO
    ): ResponseEntity<String> {
        return ResponseEntity.ok("Transaction created")
    }

    @PutMapping("/{accountId}")
    fun updateTransaction(
        @PathVariable accountId: String,
        @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO
    ): ResponseEntity<String> {
        return ResponseEntity.ok("Transaction created")
    }

    @DeleteMapping("/{accountId}/{transactionId}")
    fun deleteTransaction(
        @PathVariable accountId: String,
        @PathVariable transactionId: Long,
        @AuthenticationPrincipal authenticatedUser: UserDTO
    ): ResponseEntity<String> {
        return ResponseEntity.ok("Transaction created")
    }
}
