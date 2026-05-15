package beer.thierry.budgetplannerrest.resources

import beer.thierry.budgetplanner.api.model.transaction.CategoryAggregateDTO
import beer.thierry.budgetplanner.api.model.transaction.ImportResult
import beer.thierry.budgetplanner.api.model.transaction.ImportTransactionsRequest
import beer.thierry.budgetplanner.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.budgetplanner.api.model.transaction.TransactionDTO
import beer.thierry.budgetplanner.api.model.transaction.TransactionForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.services.transactions.ITransactionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.YearMonth
import java.time.format.DateTimeParseException
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
        @Valid @RequestBody transactionRequest: TransactionForm,
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
        @Valid @RequestBody transactionRequest: TransactionForm,
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

    @GetMapping("/{accountId}/aggregates/by-category")
    fun aggregateByCategory(
        @PathVariable accountId: String,
        @RequestParam(required = false) month: String?,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<List<CategoryAggregateDTO>> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        val yearMonth = month?.let {
            try {
                YearMonth.parse(it)
            } catch (e: DateTimeParseException) {
                return ResponseEntity.badRequest().build()
            }
        } ?: YearMonth.now()
        return ResponseEntity.ok(
            transactionService.aggregateByCategory(UUID.fromString(accountId), authenticatedUser, yearMonth)
        )
    }

    @GetMapping("/{accountId}/aggregates/by-month")
    fun aggregateByMonth(
        @PathVariable accountId: String,
        @RequestParam(defaultValue = "6") months: Int,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<List<MonthlyAggregateDTO>> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        if (months !in 1..36) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(
            transactionService.aggregateByMonth(UUID.fromString(accountId), authenticatedUser, months)
        )
    }

    @PostMapping("/{accountId}/import")
    fun importTransactions(
        @PathVariable accountId: String,
        @Valid @RequestBody request: ImportTransactionsRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO?,
    ): ResponseEntity<ImportResult> {
        if (authenticatedUser == null) return ResponseEntity.status(401).build()
        return ResponseEntity.ok(
            transactionService.importBatch(UUID.fromString(accountId), request, authenticatedUser)
        )
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
