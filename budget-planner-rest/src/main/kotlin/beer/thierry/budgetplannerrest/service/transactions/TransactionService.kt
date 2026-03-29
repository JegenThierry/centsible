package beer.thierry.budgetplannerrest.service.transactions

import beer.thierry.budgetplannerrest.model.transaction.TransactionDTO
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
}
