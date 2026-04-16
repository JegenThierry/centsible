package beer.thierry.budgetplannerrest.resources

import beer.thierry.budgetplanner.api.model.transaction.TransactionDTO
import beer.thierry.budgetplanner.api.model.transaction.TransactionForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.services.transactions.ITransactionService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RequestMapping("/api/transactions")
@RestController
class TransactionResource(private val transactionService: ITransactionService) {

    @GetMapping("/{accountId}")
    fun fetchTransactions(
        @PathVariable accountId: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<List<TransactionDTO>> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val result = transactionService.fetchTransactions(UUID.fromString(accountId), authenticatedUser, page, size)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{accountId}")
    fun createTransaction(
        @PathVariable accountId: String,
        @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<TransactionDTO> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val result =
            transactionService.createTransaction(UUID.fromString(accountId), transactionRequest, authenticatedUser)
        return ResponseEntity.ok(result)
    }

    @PutMapping("/{accountId}/{transactionId}")
    fun updateTransaction(
        @PathVariable accountId: String,
        @PathVariable transactionId: String,
        @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<TransactionDTO> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val result = transactionService.updateTransaction(
            UUID.fromString(transactionId),
            UUID.fromString(accountId),
            transactionRequest,
            authenticatedUser
        )
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/{accountId}/{transactionId}")
    fun deleteTransaction(
        @PathVariable accountId: String,
        @PathVariable transactionId: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<TransactionDTO> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val result = transactionService.deleteTransaction(
            UUID.fromString(transactionId),
            UUID.fromString(accountId),
            authenticatedUser
        )
        return ResponseEntity.ok(result)
    }
}
