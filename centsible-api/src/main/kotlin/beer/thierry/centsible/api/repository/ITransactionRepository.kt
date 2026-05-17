package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

interface ITransactionRepository {
    fun fetchTransactions(
        accountId: UUID,
        authenticatedUser: UserDTO,
        page: Int,
        pageSize: Int,
        filters: TransactionFilters = TransactionFilters(),
    ): List<TransactionDTO>
    fun fetchTransactionById(transactionId: UUID, authenticatedUser: UserDTO): TransactionDTO
    fun createTransaction(accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO): TransactionDTO
    fun updateTransaction(
        transactionId: UUID,
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO

    fun deleteTransaction(transactionId: UUID, authenticatedUser: UserDTO): TransactionDTO

    /** Returns the transactions matching [ids] that belong to [accountId] owned by [authenticatedUser]. */
    fun fetchTransactionsByIds(
        accountId: UUID,
        ids: List<UUID>,
        authenticatedUser: UserDTO,
    ): List<TransactionDTO>

    /** Deletes transactions in [ids] belonging to [accountId]. Returns count actually deleted. */
    fun deleteTransactions(
        accountId: UUID,
        ids: List<UUID>,
        authenticatedUser: UserDTO,
    ): Int

    /** Reassigns the category for transactions in [ids] belonging to [accountId]. Returns count updated. */
    fun updateCategoryForTransactions(
        accountId: UUID,
        ids: List<UUID>,
        categoryId: Int,
        authenticatedUser: UserDTO,
    ): Int

    /** Sums EXPENSE transactions per category for [accountId] within [from]..[to] (inclusive). */
    fun aggregateByCategory(
        accountId: UUID,
        authenticatedUser: UserDTO,
        from: LocalDate,
        to: LocalDate,
    ): List<CategoryAggregateDTO>

    /** Returns income and expense totals per month for the last [months] calendar months (most-recent last). */
    fun aggregateByMonth(
        accountId: UUID,
        authenticatedUser: UserDTO,
        months: Int,
    ): List<MonthlyAggregateDTO>

    /** Inserts rows skipping duplicates by [account_id, import_hash]. Returns inserted-row net adjustment. */
    fun importBatch(
        accountId: UUID,
        rows: List<ImportTransactionRow>,
        hashes: List<String>,
        authenticatedUser: UserDTO,
    ): BatchImportOutcome
}

data class BatchImportOutcome(
    val insertedCount: Int,
    val netBalanceAdjustment: BigDecimal,
)
