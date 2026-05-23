package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionsRequest
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.SetBalanceForm
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransactionSort
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.transactions.ITransactionService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@RequestMapping("/api/transactions")
@RestController
@Validated
class TransactionResource(private val transactionService: ITransactionService) {

    private val log = LoggerFactory.getLogger(TransactionResource::class.java)

    @GetMapping("/{accountId}")
    fun fetchTransactions(
        @PathVariable accountId: UUID,
        @PageableDefault(size = 25) pageable: Pageable,
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
        // Service contract is 1-based; Spring's Pageable is 0-based — translate at the boundary.
        val page = pageable.pageNumber + 1
        return ResponseEntity.ok(
            transactionService.fetchTransactions(accountId, authenticatedUser, page, pageable.pageSize, filters)
        )
    }

    @PostMapping("/{accountId}")
    fun createTransaction(
        @PathVariable accountId: UUID,
        @Valid @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<TransactionDTO> {
        val created = transactionService.createTransaction(accountId, transactionRequest, authenticatedUser)
        log.info("Created transaction id={} accountId={} userId={}", created.id, accountId, authenticatedUser.id)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{accountId}/{transactionId}")
    fun updateTransaction(
        @PathVariable accountId: UUID,
        @PathVariable transactionId: UUID,
        @Valid @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<TransactionDTO> {
        val updated = transactionService.updateTransaction(transactionId, accountId, transactionRequest, authenticatedUser)
        log.info("Updated transaction id={} accountId={} userId={}", transactionId, accountId, authenticatedUser.id)
        return ResponseEntity.ok(updated)
    }

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
        require(!to.isBefore(from)) { "toDate must not be before fromDate" }
        return ResponseEntity.ok(transactionService.aggregateByCategory(accountId, authenticatedUser, from, to))
    }

    @GetMapping("/{accountId}/aggregates/by-month")
    fun aggregateByMonth(
        @PathVariable accountId: UUID,
        @RequestParam(defaultValue = "6") @Min(1) @Max(36) months: Int,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<MonthlyAggregateDTO>> =
        ResponseEntity.ok(transactionService.aggregateByMonth(accountId, authenticatedUser, months))

    @PostMapping("/{accountId}/import")
    fun importTransactions(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: ImportTransactionsRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<ImportResult> {
        val result = transactionService.importBatch(accountId, request, authenticatedUser)
        log.info("Imported transactions accountId={} userId={} count={}", accountId, authenticatedUser.id, request.rows.size)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/{accountId}/{transactionId}")
    fun deleteTransaction(
        @PathVariable accountId: UUID,
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<TransactionDTO> {
        val deleted = transactionService.deleteTransaction(transactionId, accountId, authenticatedUser)
        log.info("Deleted transaction id={} accountId={} userId={}", transactionId, accountId, authenticatedUser.id)
        return ResponseEntity.ok(deleted)
    }

    @PostMapping("/{accountId}/bulk-delete")
    fun bulkDelete(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: BulkIdsRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<BulkResult> {
        val affected = transactionService.bulkDelete(accountId, request.ids, authenticatedUser)
        log.info("Bulk-deleted transactions accountId={} userId={} affected={}", accountId, authenticatedUser.id, affected)
        return ResponseEntity.ok(BulkResult(affected))
    }

    @PostMapping("/{accountId}/bulk-categorize")
    fun bulkCategorize(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: BulkCategorizeRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<BulkResult> {
        val affected = transactionService.bulkUpdateCategory(accountId, request.ids, request.categoryId, authenticatedUser)
        log.info(
            "Bulk-categorized transactions accountId={} userId={} categoryId={} affected={}",
            accountId, authenticatedUser.id, request.categoryId, affected,
        )
        return ResponseEntity.ok(BulkResult(affected))
    }

    @PostMapping("/{accountId}/set-balance")
    fun createBalanceAdjustment(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: SetBalanceForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<TransactionDTO> {
        val adjustment = transactionService.createBalanceAdjustment(accountId, request, authenticatedUser)
        log.info("Set balance accountId={} userId={} adjustmentTxId={}", accountId, authenticatedUser.id, adjustment.id)
        return ResponseEntity.ok(adjustment)
    }
}

data class BulkIdsRequest(val ids: List<UUID> = emptyList())
data class BulkCategorizeRequest(val ids: List<UUID> = emptyList(), val categoryId: Long = 0L)
data class BulkResult(val affected: Int)
