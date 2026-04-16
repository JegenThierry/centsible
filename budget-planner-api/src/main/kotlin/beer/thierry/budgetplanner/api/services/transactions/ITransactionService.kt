package beer.thierry.budgetplanner.api.services.transactions

import beer.thierry.budgetplanner.api.model.DEFAULT_PAGE_SIZE
import beer.thierry.budgetplanner.api.model.transaction.TransactionDTO
import beer.thierry.budgetplanner.api.model.transaction.TransactionForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
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
}
