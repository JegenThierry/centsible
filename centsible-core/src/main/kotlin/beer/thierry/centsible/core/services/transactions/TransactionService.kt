package beer.thierry.centsible.core.services.transactions

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.api.model.transaction.ImportTransactionsRequest
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.services.notifications.INotificationService
import beer.thierry.centsible.api.services.transactions.ITransactionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDate
import java.util.*

@Service
class TransactionService(
    private val transactionRepository: ITransactionRepository,
    private val accountRepository: IBudgetAccountsRepository,
    private val notificationService: INotificationService,
) : ITransactionService {

    private fun checkBudgetAlerts(user: UserDTO) {
        try {
            notificationService.maybeRaiseBudgetAlerts(user)
        } catch (e: Exception) {
            // Don't fail the transaction because alerting failed.
        }
    }

    override fun fetchTransactions(
        accountId: UUID,
        authenticatedUser: UserDTO,
        page: Int,
        size: Int,
        filters: TransactionFilters,
    ): List<TransactionDTO> {
        require(page > 0) { "page must be > 0" }
        require(size > 0) { "size must be > 0" }

        return transactionRepository.fetchTransactions(accountId, authenticatedUser, page, size, filters)
    }

    @Transactional
    override fun createTransaction(
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        val transaction = transactionRepository.createTransaction(accountId, transactionForm, authenticatedUser)
        val adjustment = calculateAdjustment(transaction.category.type, transaction.amount)
        accountRepository.updateBalance(accountId, adjustment, authenticatedUser)
        checkBudgetAlerts(authenticatedUser)
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
        val oldAdjustment = calculateAdjustment(oldTransaction.category.type, oldTransaction.amount)

        val updatedTransaction =
            transactionRepository.updateTransaction(transactionId, accountId, transactionForm, authenticatedUser)
        val newAdjustment = calculateAdjustment(updatedTransaction.category.type, updatedTransaction.amount)

        accountRepository.updateBalance(accountId, newAdjustment.subtract(oldAdjustment), authenticatedUser)
        checkBudgetAlerts(authenticatedUser)
        return updatedTransaction
    }

    @Transactional
    override fun deleteTransaction(
        transactionId: UUID,
        accountId: UUID,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        val transaction = transactionRepository.deleteTransaction(transactionId, authenticatedUser)
        val adjustment = calculateAdjustment(transaction.category.type, transaction.amount)
        accountRepository.updateBalance(accountId, adjustment.negate(), authenticatedUser)
        return transaction
    }

    @Transactional
    override fun bulkDelete(accountId: UUID, ids: List<UUID>, authenticatedUser: UserDTO): Int {
        if (ids.isEmpty()) return 0
        val transactions = transactionRepository.fetchTransactionsByIds(accountId, ids, authenticatedUser)
        val deleted = transactionRepository.deleteTransactions(accountId, transactions.map { it.id!! }, authenticatedUser)
        if (deleted == 0) return 0

        val net = transactions.fold(BigDecimal.ZERO) { acc, tx ->
            acc + calculateAdjustment(tx.category.type, tx.amount)
        }
        if (net.signum() != 0) accountRepository.updateBalance(accountId, net.negate(), authenticatedUser)
        return deleted
    }

    @Transactional
    override fun bulkUpdateCategory(
        accountId: UUID, ids: List<UUID>, categoryId: Int, authenticatedUser: UserDTO
    ): Int {
        if (ids.isEmpty()) return 0
        val oldTransactions = transactionRepository.fetchTransactionsByIds(accountId, ids, authenticatedUser)
        val updated = transactionRepository.updateCategoryForTransactions(
            accountId, oldTransactions.map { it.id!! }, categoryId, authenticatedUser
        )
        if (updated == 0) return 0

        val newTransactions = transactionRepository.fetchTransactionsByIds(
            accountId, oldTransactions.map { it.id!! }, authenticatedUser
        )
        val byId = newTransactions.associateBy { it.id!! }
        val net = oldTransactions.fold(BigDecimal.ZERO) { acc, old ->
            val new = byId[old.id!!] ?: return@fold acc
            val oldAdj = calculateAdjustment(old.category.type, old.amount)
            val newAdj = calculateAdjustment(new.category.type, new.amount)
            acc + newAdj.subtract(oldAdj)
        }
        if (net.signum() != 0) accountRepository.updateBalance(accountId, net, authenticatedUser)
        return updated
    }

    override fun aggregateByCategory(
        accountId: UUID, authenticatedUser: UserDTO, from: LocalDate, to: LocalDate
    ): List<CategoryAggregateDTO> {
        require(!to.isBefore(from)) { "to must be on or after from" }
        return transactionRepository.aggregateByCategory(accountId, authenticatedUser, from, to)
    }

    override fun aggregateByMonth(
        accountId: UUID, authenticatedUser: UserDTO, months: Int
    ): List<MonthlyAggregateDTO> =
        transactionRepository.aggregateByMonth(accountId, authenticatedUser, months)

    @Transactional
    override fun importBatch(
        accountId: UUID,
        request: ImportTransactionsRequest,
        authenticatedUser: UserDTO,
    ): ImportResult {
        val rows = request.rows
        if (rows.isEmpty()) return ImportResult(0, 0)

        val hashes = rows.map { rowHash(accountId, it) }
        val outcome = transactionRepository.importBatch(accountId, rows, hashes, authenticatedUser)

        if (outcome.netBalanceAdjustment.signum() != 0) {
            accountRepository.updateBalance(accountId, outcome.netBalanceAdjustment, authenticatedUser)
            checkBudgetAlerts(authenticatedUser)
        }

        return ImportResult(
            imported = outcome.insertedCount,
            skippedDuplicates = rows.size - outcome.insertedCount,
        )
    }

    private fun rowHash(accountId: UUID, row: ImportTransactionRow): String {
        val payload = "$accountId|${row.transactionDate}|${row.amount.toPlainString()}|${row.description}|${row.categoryId}"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun calculateAdjustment(type: CategoryType?, amount: BigDecimal?): BigDecimal {
        val value = amount ?: BigDecimal.ZERO
        return when (type) {
            CategoryType.INCOME -> value
            CategoryType.EXPENSE -> value.negate()
            else -> throw IllegalArgumentException("Invalid transaction type: $type")
        }
    }
}
