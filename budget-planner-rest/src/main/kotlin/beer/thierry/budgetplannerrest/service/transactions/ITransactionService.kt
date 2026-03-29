package beer.thierry.budgetplannerrest.service.transactions

import beer.thierry.budgetplannerrest.model.DEFAULT_PAGE_SIZE
import beer.thierry.budgetplannerrest.model.transaction.TransactionDTO
import beer.thierry.budgetplannerrest.model.user.UserDTO
import java.util.UUID

interface ITransactionService {
    fun fetchTransactions(accountId: UUID, authenticatedUser: UserDTO, page: Int, size: Int = DEFAULT_PAGE_SIZE): List<TransactionDTO>
}
