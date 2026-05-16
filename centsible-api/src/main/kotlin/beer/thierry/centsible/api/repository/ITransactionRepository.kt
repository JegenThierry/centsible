package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.time.YearMonth
import java.util.*

interface ITransactionRepository {
    fun fetchTransactions(accountId: UUID, authenticatedUser: UserDTO, page: Int, pageSize: Int): List<TransactionDTO>
    fun fetchTransactionById(transactionId: UUID, authenticatedUser: UserDTO): TransactionDTO
    fun createTransaction(accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO): TransactionDTO
    fun updateTransaction(
        transactionId: UUID,
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO

    fun deleteTransaction(transactionId: UUID, authenticatedUser: UserDTO): TransactionDTO

    /** Sums EXPENSE transactions per category for [accountId] within [yearMonth]. */
    fun aggregateByCategory(
        accountId: UUID,
        authenticatedUser: UserDTO,
        yearMonth: YearMonth,
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
