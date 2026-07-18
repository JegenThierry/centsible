package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.DailyAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionsRequest
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.SetBalanceForm
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransactionSort
import beer.thierry.centsible.api.model.transaction.TransferDetailsDTO
import beer.thierry.centsible.api.model.transaction.TransferForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.transactions.ITransactionService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
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
        @RequestParam(required = false) tagIds: List<Long>?,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
        @RequestParam(required = false) type: CategoryType?,
        @RequestParam(required = false) amountMin: BigDecimal?,
        @RequestParam(required = false) amountMax: BigDecimal?,
        @RequestParam(required = false) sort: TransactionSort?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<TransactionDTO> {
        val filters = TransactionFilters(
            search = search,
            categoryIds = categoryIds,
            tagIds = tagIds,
            from = fromDate,
            to = toDate,
            type = type,
            amountMin = amountMin,
            amountMax = amountMax,
            sort = sort ?: TransactionSort.DATE_DESC,
        )
        val page = pageable.pageNumber + 1
        return transactionService.fetchTransactions(accountId, authenticatedUser, page, pageable.pageSize, filters)
    }

    @PostMapping("/{accountId}")
    fun createTransaction(
        @PathVariable accountId: UUID,
        @Valid @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): TransactionDTO {
        val created = transactionService.createTransaction(accountId, transactionRequest, authenticatedUser)
        log.info("Created transaction id={} accountId={} userId={}", created.id, accountId, authenticatedUser.id)
        return created
    }

    @PutMapping("/{accountId}/{transactionId}")
    fun updateTransaction(
        @PathVariable accountId: UUID,
        @PathVariable transactionId: UUID,
        @Valid @RequestBody transactionRequest: TransactionForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): TransactionDTO {
        val updated = transactionService.updateTransaction(transactionId, accountId, transactionRequest, authenticatedUser)
        log.info("Updated transaction id={} accountId={} userId={}", transactionId, accountId, authenticatedUser.id)
        return updated
    }

    @PostMapping("/{accountId}/transfer")
    fun createTransfer(
        @PathVariable accountId: UUID,
        @Valid @RequestBody form: TransferForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<TransactionDTO> {
        val legs = transactionService.createTransfer(accountId, form, authenticatedUser)
        log.info("Created transfer sourceAccountId={} userId={} legs={}", accountId, authenticatedUser.id, legs.size)
        return legs
    }

    @PutMapping("/{accountId}/transfer/{transactionId}")
    fun updateTransfer(
        @PathVariable accountId: UUID,
        @PathVariable transactionId: UUID,
        @Valid @RequestBody form: TransferForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<TransactionDTO> {
        val legs = transactionService.updateTransfer(transactionId, accountId, form, authenticatedUser)
        log.info("Updated transfer txId={} sourceAccountId={} userId={}", transactionId, accountId, authenticatedUser.id)
        return legs
    }

    @GetMapping("/transfer/{transactionId}")
    fun fetchTransfer(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): TransferDetailsDTO =
        transactionService.fetchTransfer(transactionId, authenticatedUser)

    @GetMapping("/{accountId}/aggregates/by-category")
    fun aggregateByCategory(
        @PathVariable accountId: UUID,
        @RequestParam(required = false) month: YearMonth?,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<CategoryAggregateDTO> {
        val (from, to) = when {
            fromDate != null && toDate != null -> fromDate to toDate
            month != null -> month.atDay(1) to month.atEndOfMonth()
            else -> YearMonth.now().let { it.atDay(1) to it.atEndOfMonth() }
        }
        require(!to.isBefore(from)) { "toDate must not be before fromDate" }
        return transactionService.aggregateByCategory(accountId, authenticatedUser, from, to)
    }

    @GetMapping("/{accountId}/aggregates/by-month")
    fun aggregateByMonth(
        @PathVariable accountId: UUID,
        @RequestParam(defaultValue = "6") @Min(1) @Max(36) months: Int,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<MonthlyAggregateDTO> =
        transactionService.aggregateByMonth(accountId, authenticatedUser, months)

    @GetMapping("/{accountId}/aggregates/by-day")
    fun aggregateByDay(
        @PathVariable accountId: UUID,
        @RequestParam(defaultValue = "371") @Min(1) @Max(731) days: Int,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<DailyAggregateDTO> =
        transactionService.aggregateByDay(accountId, authenticatedUser, days)

    @PostMapping("/{accountId}/import")
    fun importTransactions(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: ImportTransactionsRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ImportResult {
        val result = transactionService.importBatch(accountId, request, authenticatedUser)
        log.info("Imported transactions accountId={} userId={} count={}", accountId, authenticatedUser.id, request.rows.size)
        return result
    }

    @DeleteMapping("/{accountId}/{transactionId}")
    fun deleteTransaction(
        @PathVariable accountId: UUID,
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): TransactionDTO {
        val deleted = transactionService.deleteTransaction(transactionId, accountId, authenticatedUser)
        log.info("Deleted transaction id={} accountId={} userId={}", transactionId, accountId, authenticatedUser.id)
        return deleted
    }

    @PostMapping("/{accountId}/bulk-delete")
    fun bulkDelete(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: BulkIdsRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): BulkResult {
        val affected = transactionService.bulkDelete(accountId, request.ids, authenticatedUser)
        log.info("Bulk-deleted transactions accountId={} userId={} affected={}", accountId, authenticatedUser.id, affected)
        return BulkResult(affected)
    }

    @PostMapping("/{accountId}/bulk-categorize")
    fun bulkCategorize(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: BulkCategorizeRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): BulkResult {
        val affected = transactionService.bulkUpdateCategory(accountId, request.ids, request.categoryId, authenticatedUser)
        log.info(
            "Bulk-categorized transactions accountId={} userId={} categoryId={} affected={}",
            accountId, authenticatedUser.id, request.categoryId, affected,
        )
        return BulkResult(affected)
    }

    @PostMapping("/{accountId}/set-balance")
    fun createBalanceAdjustment(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: SetBalanceForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): TransactionDTO {
        val adjustment = transactionService.createBalanceAdjustment(accountId, request, authenticatedUser)
        log.info("Set balance accountId={} userId={} adjustmentTxId={}", accountId, authenticatedUser.id, adjustment.id)
        return adjustment
    }

    @GetMapping("/{accountId}/conversion-preview")
    fun previewConversion(
        @PathVariable accountId: UUID,
        @RequestParam amount: BigDecimal,
        @RequestParam currency: Currency,
        @RequestParam(required = false) date: LocalDate?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ConversionResult =
        transactionService.previewConversion(accountId, amount, currency, date ?: LocalDate.now(), authenticatedUser)
}

data class BulkIdsRequest(val ids: List<UUID> = emptyList())
data class BulkCategorizeRequest(val ids: List<UUID> = emptyList(), val categoryId: Long = 0L)
data class BulkResult(val affected: Int)
