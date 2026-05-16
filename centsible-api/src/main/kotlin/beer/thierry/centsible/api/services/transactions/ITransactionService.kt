package beer.thierry.centsible.api.services.transactions

import beer.thierry.centsible.api.model.DEFAULT_PAGE_SIZE
import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionsRequest
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.YearMonth
import java.util.*

interface ITransactionService {
    fun fetchTransactions(
        accountId: UUID,
        authenticatedUser: UserDTO,
        page: Int,
        size: Int = DEFAULT_PAGE_SIZE
    ): List<TransactionDTO>

    fun createTransaction(accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO): TransactionDTO
    fun updateTransaction(
        transactionId: UUID,
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO

    fun deleteTransaction(transactionId: UUID, accountId: UUID, authenticatedUser: UserDTO): TransactionDTO

    fun aggregateByCategory(
        accountId: UUID,
        authenticatedUser: UserDTO,
        yearMonth: YearMonth = YearMonth.now(),
    ): List<CategoryAggregateDTO>

    fun aggregateByMonth(
        accountId: UUID,
        authenticatedUser: UserDTO,
        months: Int = 6,
    ): List<MonthlyAggregateDTO>

    fun importBatch(
        accountId: UUID,
        request: ImportTransactionsRequest,
        authenticatedUser: UserDTO,
    ): ImportResult
}
