package beer.thierry.budgetplannerrest.service.account

import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplannerrest.model.transaction.TransactionType
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.repository.accounts.IBudgetAccountsRepository
import beer.thierry.budgetplannerrest.repository.transactions.ITransactionRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class BudgetBudgetAccountService(
    private val accountRepository: IBudgetAccountsRepository,
    private val transactionRepository: ITransactionRepository
) : IBudgetAccountService {

    override fun createAccount(
        createBudgetAccountRequest: CreateBudgetAccountRequest,
        authenticatedUser: UserDTO,
    ): BudgetAccountDTO {
        return accountRepository.createAccount(authenticatedUser, createBudgetAccountRequest)
    }

    override fun fetchAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO> {
        return accountRepository.fetchAllAccounts(authenticatedUser)
    }

    override fun fetchAccountById(id: String, authenticatedUser: UserDTO): BudgetAccountDTO {
        return accountRepository.fetchAccountById(UUID.fromString(id), authenticatedUser)
    }

    override fun fetchAccountSnapshots(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO> {
        val uuid = UUID.fromString(accountId)
        val initialBalance = accountRepository.fetchInitialBalance(uuid, authenticatedUser)
        val transactions = transactionRepository.fetchTransactionsUntilDate(uuid, endDate, authenticatedUser)

        val snapshots = mutableListOf<BudgetAccountSnapshotDTO>()
        var currentBalance = initialBalance

        val transactionsBeforeStart = transactions.filter { it.transactionDate!!.isBefore(startDate) }
        transactionsBeforeStart.forEach { t ->
            when (t.type) {
                TransactionType.INCOME -> currentBalance = currentBalance.add(t.amount ?: BigDecimal.ZERO)
                TransactionType.EXPENSE -> currentBalance = currentBalance.subtract(t.amount ?: BigDecimal.ZERO)
                else -> throw IllegalStateException("Invalid transaction type: ${t.type}")
            }
        }

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dailyTransactions = transactions.filter { it.transactionDate == currentDate }
            dailyTransactions.forEach { t ->
                when (t.type) {
                    TransactionType.INCOME -> currentBalance = currentBalance.add(t.amount ?: BigDecimal.ZERO)
                    TransactionType.EXPENSE -> currentBalance = currentBalance.subtract(t.amount ?: BigDecimal.ZERO)
                    else -> throw IllegalStateException("Invalid transaction type: ${t.type}")
                }
            }

            snapshots.add(
                BudgetAccountSnapshotDTO(
                    date = currentDate,
                    balance = currentBalance,
                    transactions = dailyTransactions
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        return snapshots
    }
}
