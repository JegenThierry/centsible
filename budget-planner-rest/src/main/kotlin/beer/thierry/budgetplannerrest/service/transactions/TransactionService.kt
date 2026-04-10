package beer.thierry.budgetplannerrest.service.transactions

import beer.thierry.budgetplannerrest.model.transaction.TransactionDTO
import beer.thierry.budgetplannerrest.model.transaction.TransactionForm
import beer.thierry.budgetplannerrest.model.transaction.TransactionType
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.repository.accounts.IBudgetAccountsRepository
import beer.thierry.budgetplannerrest.repository.accounthistory.IBudgetAccountHistoryRepository
import beer.thierry.budgetplannerrest.repository.transactions.ITransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class TransactionService(
    private val transactionRepository: ITransactionRepository,
    private val accountRepository: IBudgetAccountsRepository,
    private val accountHistoryRepository: IBudgetAccountHistoryRepository
) : ITransactionService {

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

    @Transactional
    override fun createTransaction(
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        val transaction = transactionRepository.createTransaction(accountId, transactionForm, authenticatedUser)
        val adjustment = calculateAdjustment(transaction.type, transaction.amount)
        updateAccountBalanceAndLogHistory(accountId, adjustment, authenticatedUser)

        return transaction
    }

    @Transactional
    override fun updateTransaction(
        transactionId: UUID,
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        val oldTransaction = transactionRepository.fetchTransactionById(transactionId, authenticatedUser)
        val oldAdjustment = calculateAdjustment(oldTransaction.type, oldTransaction.amount)

        val updatedTransaction =
            transactionRepository.updateTransaction(transactionId, accountId, transactionForm, authenticatedUser)
        val newAdjustment = calculateAdjustment(updatedTransaction.type, updatedTransaction.amount)

        updateAccountBalanceAndLogHistory(accountId, oldAdjustment.add(newAdjustment), authenticatedUser)

        return updatedTransaction
    }

    @Transactional
    override fun deleteTransaction(
        transactionId: UUID,
        accountId: UUID,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        val transaction = transactionRepository.deleteTransaction(transactionId, authenticatedUser)
        val adjustment = calculateAdjustment(transaction.type, transaction.amount)
        updateAccountBalanceAndLogHistory(accountId, adjustment, authenticatedUser)

        return transaction
    }

    private fun calculateAdjustment(type: TransactionType?, amount: BigDecimal?): BigDecimal {
        val value = amount ?: BigDecimal.ZERO
        return when (type) {
            TransactionType.INCOME -> value
            TransactionType.EXPENSE -> value.negate()
            else -> throw IllegalArgumentException("Invalid transaction type: $type")
        }
    }

    private fun updateAccountBalanceAndLogHistory(accountId: UUID, adjustment: BigDecimal, authenticatedUser: UserDTO) {
        accountRepository.updateBalance(accountId, adjustment)
        val account = accountRepository.fetchAccountById(accountId, authenticatedUser)
        accountHistoryRepository.logAccountHistory(account, authenticatedUser)
    }
}
