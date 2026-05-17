package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionsRequest
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransactionSort
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.transactions.ITransactionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@RequestMapping("/api/transactions")
@RestController
class TransactionResource(private val transactionService: ITransactionService) {

    @GetMapping("/{accountId}")
    fun fetchTransactions(
        @PathVariable accountId: UUID,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
        @RequestParam(required = false) sort: TransactionSort?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<TransactionDTO>> {
        val filters = TransactionFilters(
            search = search,
            categoryIds = categoryIds,
            from = fromDate,
            to = toDate,
            sort = sort ?: TransactionSort.DATE_DESC,
        )
        return ResponseEntity.ok(transactionService.fetchTransactions(accountId, authenticatedUser, page, size, filters))
    }

    @PostMapping("/{accountId}")
    fun createTransaction(
        @PathVariable accountId: UUID,
        @Valid @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<TransactionDTO> =
        ResponseEntity.ok(transactionService.createTransaction(accountId, transactionRequest, authenticatedUser))

    @PutMapping("/{accountId}/{transactionId}")
    fun updateTransaction(
        @PathVariable accountId: UUID,
        @PathVariable transactionId: UUID,
        @Valid @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<TransactionDTO> =
        ResponseEntity.ok(
            transactionService.updateTransaction(transactionId, accountId, transactionRequest, authenticatedUser)
        )

    @GetMapping("/{accountId}/aggregates/by-category")
    fun aggregateByCategory(
        @PathVariable accountId: UUID,
        @RequestParam(required = false) month: YearMonth?,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<CategoryAggregateDTO>> {
        val (from, to) = when {
            fromDate != null && toDate != null -> fromDate to toDate
            month != null -> month.atDay(1) to month.atEndOfMonth()
            else -> YearMonth.now().let { it.atDay(1) to it.atEndOfMonth() }
        }
        if (to.isBefore(from)) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(transactionService.aggregateByCategory(accountId, authenticatedUser, from, to))
    }

    @GetMapping("/{accountId}/aggregates/by-month")
    fun aggregateByMonth(
        @PathVariable accountId: UUID,
        @RequestParam(defaultValue = "6") months: Int,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<MonthlyAggregateDTO>> {
        if (months !in 1..36) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(transactionService.aggregateByMonth(accountId, authenticatedUser, months))
    }

    @PostMapping("/{accountId}/import")
    fun importTransactions(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: ImportTransactionsRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ImportResult> =
        ResponseEntity.ok(transactionService.importBatch(accountId, request, authenticatedUser))

    @DeleteMapping("/{accountId}/{transactionId}")
    fun deleteTransaction(
        @PathVariable accountId: UUID,
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<TransactionDTO> =
        ResponseEntity.ok(
            transactionService.deleteTransaction(transactionId, accountId, authenticatedUser)
        )

    @PostMapping("/{accountId}/bulk-delete")
    fun bulkDelete(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: BulkIdsRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<BulkResult> =
        ResponseEntity.ok(BulkResult(transactionService.bulkDelete(accountId, request.ids, authenticatedUser)))

    @PostMapping("/{accountId}/bulk-categorize")
    fun bulkCategorize(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: BulkCategorizeRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<BulkResult> =
        ResponseEntity.ok(
            BulkResult(transactionService.bulkUpdateCategory(accountId, request.ids, request.categoryId, authenticatedUser))
        )
}

data class BulkIdsRequest(val ids: List<UUID> = emptyList())
data class BulkCategorizeRequest(val ids: List<UUID> = emptyList(), val categoryId: Long = 0L)
data class BulkResult(val affected: Int)
