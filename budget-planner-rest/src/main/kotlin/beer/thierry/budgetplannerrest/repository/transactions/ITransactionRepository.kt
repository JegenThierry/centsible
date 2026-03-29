package beer.thierry.budgetplannerrest.repository.transactions

import beer.thierry.budgetplannerrest.model.transaction.TransactionDTO
import beer.thierry.budgetplannerrest.model.transaction.TransactionForm
import beer.thierry.budgetplannerrest.model.user.UserDTO
import java.util.UUID

interface ITransactionRepository {
    fun fetchTransactions(accountId: UUID, authenticatedUser: UserDTO, page: Int, pageSize: Int): List<TransactionDTO>
    fun fetchTransactionById(transactionId: UUID, accountId: UUID, authenticatedUser: UserDTO): TransactionDTO
    fun createTransaction(accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO): TransactionDTO
    fun updateTransaction(transactionId: UUID, accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO): TransactionDTO
    fun deleteTransaction(transactionId: UUID, authenticatedUser: UserDTO): TransactionDTO
}
