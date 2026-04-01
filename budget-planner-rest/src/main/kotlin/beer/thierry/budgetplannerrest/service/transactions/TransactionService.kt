package beer.thierry.budgetplannerrest.service.transactions

import beer.thierry.budgetplannerrest.model.transaction.TransactionDTO
import beer.thierry.budgetplannerrest.model.transaction.TransactionForm
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.repository.transactions.ITransactionRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TransactionService(private val transactionRepository: ITransactionRepository) : ITransactionService {

    override fun fetchTransactions(
        accountId: UUID,
        authenticatedUser: UserDTO,
        page: Int,
        size: Int
    ): List<TransactionDTO> {
        require(page > 0) { "page must be > 0" }
        require(size > 0) { "size must be > 0" }

        return transactionRepository.fetchTransactions(accountId, authenticatedUser, page, size)
    }

    override fun createTransaction(
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        return transactionRepository.createTransaction(accountId, transactionForm, authenticatedUser)
    }

    override fun updateTransaction(
        transactionId: UUID,
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        return transactionRepository.updateTransaction(transactionId, accountId, transactionForm, authenticatedUser)
    }

    override fun deleteTransaction(
        transactionId: UUID,
        accountId: UUID,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        return transactionRepository.deleteTransaction(transactionId, authenticatedUser)
    }
}
