package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.transaction.ImportTransactionRow
import beer.thierry.budgetplanner.api.model.transaction.TransactionDTO
import beer.thierry.budgetplanner.api.model.transaction.TransactionForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.math.BigDecimal
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
