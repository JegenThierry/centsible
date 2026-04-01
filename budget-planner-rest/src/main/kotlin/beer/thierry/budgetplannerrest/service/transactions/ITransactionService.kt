package beer.thierry.budgetplannerrest.service.transactions

import beer.thierry.budgetplannerrest.model.DEFAULT_PAGE_SIZE
import beer.thierry.budgetplannerrest.model.transaction.TransactionDTO
import beer.thierry.budgetplannerrest.model.transaction.TransactionForm
import beer.thierry.budgetplannerrest.model.user.UserDTO
import java.util.UUID

interface ITransactionService {
    fun fetchTransactions(accountId: UUID, authenticatedUser: UserDTO, page: Int, size: Int = DEFAULT_PAGE_SIZE): List<TransactionDTO>
    fun createTransaction(accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO): TransactionDTO
    fun updateTransaction(transactionId: UUID, accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO): TransactionDTO
    fun deleteTransaction(transactionId: UUID, accountId: UUID, authenticatedUser: UserDTO): TransactionDTO
}
