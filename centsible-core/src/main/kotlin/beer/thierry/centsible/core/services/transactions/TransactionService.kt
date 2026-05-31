package beer.thierry.centsible.core.services.transactions

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.api.model.transaction.ImportTransactionsRequest
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.SetBalanceForm
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.CategoryClassification
import beer.thierry.centsible.api.repository.IAttachmentRepository
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.services.notifications.INotificationService
import beer.thierry.centsible.api.services.transactions.ITransactionService
import org.slf4j.LoggerFactory
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
    private val categoriesRepository: ICategoriesRepository,
    private val attachmentRepository: IAttachmentRepository,
    private val notificationService: INotificationService,
) : ITransactionService {

    private val log = LoggerFactory.getLogger(TransactionService::class.java)

    // Alerting must never fail a transaction commit, so swallow with a log instead of letting it propagate.
    private fun checkBudgetAlerts(user: UserDTO, categoryIds: Collection<Long>? = null) {
        try {
            notificationService.maybeRaiseBudgetAlerts(user, categoryIds)
        } catch (e: Exception) {
            log.warn("Budget alert evaluation failed for user {}", user.id, e)
        }
    }

    private fun checkInlineTransactionAlerts(user: UserDTO, tx: TransactionDTO, accountId: UUID) {
        val id = tx.id ?: return
        val amount = tx.amount ?: return
        try {
            notificationService.evaluateTransactionAlerts(user, id, amount, tx.description, accountId)
        } catch (e: Exception) {
            log.warn("Inline transaction alert evaluation failed for user {}", user.id, e)
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

        val transactions = transactionRepository.fetchTransactions(accountId, authenticatedUser, page, size, filters)
        val ids = transactions.mapNotNull { it.id }
        if (ids.isEmpty()) return transactions

        val counts = attachmentRepository.countByTransactionIds(authenticatedUser, ids)
        transactions.forEach { it.attachmentCount = counts[it.id] ?: 0 }
        return transactions
    }

    @Transactional
    override fun createTransaction(
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        val resolvedForm = transactionForm.copy(
            type = resolveType(authenticatedUser, transactionForm.categoryId, transactionForm.type)
        )
        val transaction = transactionRepository.createTransaction(accountId, resolvedForm, authenticatedUser)
        val adjustment = calculateAdjustment(transaction.type, transaction.amount)
        accountRepository.updateBalance(accountId, adjustment, authenticatedUser)
        checkBudgetAlerts(authenticatedUser, listOf(resolvedForm.categoryId))
        checkInlineTransactionAlerts(authenticatedUser, transaction, accountId)
        log.info(
            "Created transaction id={} accountId={} userId={} type={} amount={}",
            transaction.id, accountId, authenticatedUser.id, transaction.type, transaction.amount,
        )
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

        val resolvedForm = transactionForm.copy(
            type = resolveType(authenticatedUser, transactionForm.categoryId, transactionForm.type)
        )
        val updatedTransaction =
            transactionRepository.updateTransaction(transactionId, accountId, resolvedForm, authenticatedUser)
        val newAdjustment = calculateAdjustment(updatedTransaction.type, updatedTransaction.amount)

        accountRepository.updateBalance(accountId, newAdjustment.subtract(oldAdjustment), authenticatedUser)
        checkBudgetAlerts(authenticatedUser, setOf(oldTransaction.category.id, updatedTransaction.category.id).filterNotNull())
        checkInlineTransactionAlerts(authenticatedUser, updatedTransaction, accountId)
        log.info(
            "Updated transaction id={} accountId={} userId={} type={} amount={}",
            transactionId, accountId, authenticatedUser.id, updatedTransaction.type, updatedTransaction.amount,
        )
        return updatedTransaction
    }

    @Transactional
    override fun deleteTransaction(
        transactionId: UUID,
        accountId: UUID,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        val transaction = transactionRepository.deleteTransaction(transactionId, accountId, authenticatedUser)
        val adjustment = calculateAdjustment(transaction.type, transaction.amount)
        accountRepository.updateBalance(accountId, adjustment.negate(), authenticatedUser)
        log.info(
            "Deleted transaction id={} accountId={} userId={}",
            transactionId, accountId, authenticatedUser.id,
        )
        return transaction
    }

    @Transactional
    override fun bulkDelete(accountId: UUID, ids: List<UUID>, authenticatedUser: UserDTO): Int {
        if (ids.isEmpty()) return 0
        val transactions = transactionRepository.fetchTransactionsByIds(accountId, ids, authenticatedUser)
        val deleted = transactionRepository.deleteTransactions(accountId, transactions.map { it.id!! }, authenticatedUser)
        if (deleted == 0) return 0

        val net = transactions.fold(BigDecimal.ZERO) { acc, tx ->
            acc + calculateAdjustment(tx.type, tx.amount)
        }
        if (net.signum() != 0) accountRepository.updateBalance(accountId, net.negate(), authenticatedUser)
        checkBudgetAlerts(authenticatedUser, transactions.mapNotNull { it.category.id }.distinct())
        log.info(
            "Bulk-deleted transactions count={} accountId={} userId={}",
            deleted, accountId, authenticatedUser.id,
        )
        return deleted
    }

    @Transactional
    override fun bulkUpdateCategory(
        accountId: UUID, ids: List<UUID>, categoryId: Long, authenticatedUser: UserDTO
    ): Int {
        if (ids.isEmpty()) return 0
        val oldTransactions = transactionRepository.fetchTransactionsByIds(accountId, ids, authenticatedUser)
        val updated = transactionRepository.updateCategoryForTransactions(
            accountId, oldTransactions.map { it.id!! }, categoryId, authenticatedUser
        )
        if (updated == 0) return 0

        val touched = (oldTransactions.mapNotNull { it.category.id } + categoryId).distinct()
        checkBudgetAlerts(authenticatedUser, touched)
        log.info(
            "Bulk-updated transaction category count={} categoryId={} accountId={} userId={}",
            updated, categoryId, accountId, authenticatedUser.id,
        )
        return updated
    }

    override fun aggregateByCategory(
        accountId: UUID, authenticatedUser: UserDTO, from: LocalDate, to: LocalDate
    ): List<CategoryAggregateDTO> =
        transactionRepository.aggregateByCategory(accountId, authenticatedUser, from, to)

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

        val classifications = categoriesRepository.fetchCategoryClassifications(
            authenticatedUser, rows.mapTo(HashSet()) { it.categoryId }
        )
        val resolvedRows = rows.map { row ->
            val classification = classifications[row.categoryId] ?: throw categoryNotFound(row.categoryId)
            row.copy(type = resolveType(classification, row.type))
        }

        val hashes = resolvedRows.map { rowHash(accountId, it) }
        val outcome = transactionRepository.importBatch(accountId, resolvedRows, hashes, authenticatedUser)

        if (outcome.netBalanceAdjustment.signum() != 0) {
            accountRepository.updateBalance(accountId, outcome.netBalanceAdjustment, authenticatedUser)
            checkBudgetAlerts(authenticatedUser, resolvedRows.map { it.categoryId }.distinct())
        }

        log.info(
            "Imported transaction batch accountId={} userId={} totalRows={} inserted={} skippedDuplicates={}",
            accountId, authenticatedUser.id, rows.size, outcome.insertedCount, rows.size - outcome.insertedCount,
        )
        return ImportResult(
            imported = outcome.insertedCount,
            skippedDuplicates = rows.size - outcome.insertedCount,
        )
    }

    @Transactional
    override fun createBalanceAdjustment(
        accountId: UUID,
        form: SetBalanceForm,
        authenticatedUser: UserDTO,
    ): TransactionDTO {
        val account = accountRepository.fetchAccountById(accountId, authenticatedUser)
        val delta = form.newBalance.subtract(account.balance)
        if (delta.signum() == 0) {
            throw IllegalArgumentException("New balance must differ from the current balance")
        }

        val adjustmentForm = TransactionForm(
            amount = delta.abs(),
            categoryId = form.categoryId,
            description = form.description,
            transactionDate = form.transactionDate,
            type = if (delta.signum() > 0) CategoryType.INCOME else CategoryType.EXPENSE,
        )
        return createTransaction(accountId, adjustmentForm, authenticatedUser)
    }

    private fun rowHash(accountId: UUID, row: ImportTransactionRow): String {
        val payload = "$accountId|${row.transactionDate}|${row.amount.toPlainString()}|${row.description}|${row.categoryId}"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun resolveType(
        user: UserDTO,
        categoryId: Long,
        requestedType: CategoryType?,
    ): CategoryType {
        val classification = categoriesRepository.fetchCategoryClassifications(user, listOf(categoryId))[categoryId]
            ?: throw categoryNotFound(categoryId)
        return resolveType(classification, requestedType)
    }

    // Managed categories (Lending / Repayment) must keep their type to preserve domain invariants.
    private fun resolveType(classification: CategoryClassification, requestedType: CategoryType?): CategoryType =
        if (classification.isManaged) classification.type else requestedType ?: classification.type

    private fun categoryNotFound(categoryId: Long): IllegalArgumentException =
        IllegalArgumentException("Category $categoryId not found or not accessible")

    private fun calculateAdjustment(type: CategoryType?, amount: BigDecimal?): BigDecimal {
        val value = amount ?: BigDecimal.ZERO
        return when (type) {
            CategoryType.INCOME -> value
            CategoryType.EXPENSE -> value.negate()
            else -> throw IllegalArgumentException("Invalid transaction type: $type")
        }
    }
}
