package beer.thierry.centsible.core.services.transactions

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.categorization.firstMatch
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.integrations.ImportedTransactionDTO
import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.api.model.transaction.ImportTransactionsRequest
import beer.thierry.centsible.api.model.transaction.DailyAggregateDTO
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.SetBalanceForm
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransferDetailsDTO
import beer.thierry.centsible.api.model.transaction.TransferForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.CategoryClassification
import beer.thierry.centsible.api.repository.IAttachmentRepository
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.services.categorization.ICategorizationService
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.api.services.notifications.INotificationService
import beer.thierry.centsible.api.services.transactions.ITransactionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDate
import java.util.*

private const val UNCATEGORIZED_SYSTEM_KEY = "UNCATEGORIZED"

@Service
class TransactionService(
    private val transactionRepository: ITransactionRepository,
    private val accountRepository: IBudgetAccountsRepository,
    private val categoriesRepository: ICategoriesRepository,
    private val attachmentRepository: IAttachmentRepository,
    private val notificationService: INotificationService,
    private val categorizationService: ICategorizationService,
    private val currencyConversionService: ICurrencyConversionService,
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
        val account = accountRepository.fetchAccountById(accountId, authenticatedUser)
        val conversion = currencyConversionService.convert(
            transactionForm.amount,
            transactionForm.currency ?: account.currency,
            account.currency,
            transactionForm.transactionDate,
        )
        val resolvedForm = transactionForm.copy(
            type = resolveType(authenticatedUser, transactionForm.categoryId, transactionForm.type)
        )
        val transaction = transactionRepository.createTransaction(accountId, resolvedForm, conversion, authenticatedUser)
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

        val account = accountRepository.fetchAccountById(accountId, authenticatedUser)
        val conversion = currencyConversionService.convert(
            transactionForm.amount,
            transactionForm.currency ?: account.currency,
            account.currency,
            transactionForm.transactionDate,
        )
        val resolvedForm = transactionForm.copy(
            type = resolveType(authenticatedUser, transactionForm.categoryId, transactionForm.type)
        )
        val updatedTransaction =
            transactionRepository.updateTransaction(transactionId, accountId, resolvedForm, conversion, authenticatedUser)
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
        val deleted = transactionRepository.deleteTransaction(transactionId, accountId, authenticatedUser)
        val groupId = deleted.transferGroupId
        if (groupId != null) {
            val remaining = transactionRepository.fetchTransferLegs(groupId, authenticatedUser)
            if (remaining.isNotEmpty()) {
                transactionRepository.deleteTransactionsByIds(remaining.map { it.id }, authenticatedUser)
            }
            accountRepository.updateBalance(
                accountId, calculateAdjustment(deleted.type, deleted.amount).negate(), authenticatedUser
            )
            remaining.forEach { leg ->
                accountRepository.updateBalance(
                    leg.accountId, calculateAdjustment(leg.type, leg.amount).negate(), authenticatedUser
                )
            }
            log.info("Deleted transfer groupId={} legs={} userId={}", groupId, remaining.size + 1, authenticatedUser.id)
            return deleted
        }

        val adjustment = calculateAdjustment(deleted.type, deleted.amount)
        accountRepository.updateBalance(accountId, adjustment.negate(), authenticatedUser)
        log.info(
            "Deleted transaction id={} accountId={} userId={}",
            transactionId, accountId, authenticatedUser.id,
        )
        return deleted
    }

    @Transactional
    override fun createTransfer(
        sourceAccountId: UUID,
        form: TransferForm,
        authenticatedUser: UserDTO,
    ): List<TransactionDTO> {
        val destinationAccountId = form.destinationAccountId
            ?: throw LocalizedException.BadRequest("error.transfer.destinationRequired")
        if (sourceAccountId == destinationAccountId) throw LocalizedException.BadRequest("error.transfer.sameAccount")

        val source = accountRepository.fetchAccountById(sourceAccountId, authenticatedUser)
        val destination = accountRepository.fetchAccountById(destinationAccountId, authenticatedUser)

        val conversion = currencyConversionService.convert(
            form.amount, source.currency, destination.currency, form.transactionDate
        )

        val legs = transactionRepository.insertTransfer(
            sourceAccountId, destinationAccountId, form, conversion, authenticatedUser
        )
        accountRepository.updateBalance(sourceAccountId, form.amount.negate(), authenticatedUser)
        accountRepository.updateBalance(destinationAccountId, conversion.convertedAmount, authenticatedUser)

        log.info(
            "Created transfer groupId={} sourceAccountId={} destinationAccountId={} userId={} amount={}",
            legs.firstOrNull()?.transferGroupId, sourceAccountId, destinationAccountId, authenticatedUser.id, form.amount,
        )
        return legs
    }

    @Transactional
    override fun updateTransfer(
        transactionId: UUID,
        sourceAccountId: UUID,
        form: TransferForm,
        authenticatedUser: UserDTO,
    ): List<TransactionDTO> {
        val destinationAccountId = form.destinationAccountId
            ?: throw LocalizedException.BadRequest("error.transfer.destinationRequired")
        if (sourceAccountId == destinationAccountId) throw LocalizedException.BadRequest("error.transfer.sameAccount")

        val existing = transactionRepository.fetchTransactionById(transactionId, authenticatedUser)
        val groupId = existing.transferGroupId
            ?: throw LocalizedException.BadRequest("error.transfer.notATransfer")

        val oldLegs = transactionRepository.fetchTransferLegs(groupId, authenticatedUser)
        val sourceLeg = oldLegs.firstOrNull { it.type == CategoryType.EXPENSE }
            ?: throw LocalizedException.BadRequest("error.transfer.legNotFound")
        val destinationLeg = oldLegs.firstOrNull { it.type == CategoryType.INCOME }
            ?: throw LocalizedException.BadRequest("error.transfer.legNotFound")

        val source = accountRepository.fetchAccountById(sourceAccountId, authenticatedUser)
        val destination = accountRepository.fetchAccountById(destinationAccountId, authenticatedUser)
        val conversion = currencyConversionService.convert(
            form.amount, source.currency, destination.currency, form.transactionDate
        )

        // Reverse the old legs' effect on their (possibly different) accounts...
        oldLegs.forEach { leg ->
            accountRepository.updateBalance(
                leg.accountId, calculateAdjustment(leg.type, leg.amount).negate(), authenticatedUser
            )
        }

        // ...mutate both legs in place — ids, transferGroupId, attachments and createdAt survive...
        val legs = transactionRepository.updateTransfer(
            sourceLegId = sourceLeg.id,
            destinationLegId = destinationLeg.id,
            sourceAccountId = sourceAccountId,
            destinationAccountId = destinationAccountId,
            form = form,
            conversion = conversion,
            authenticatedUser = authenticatedUser,
        )

        // ...then apply the new amounts to the (new) source and destination accounts.
        accountRepository.updateBalance(sourceAccountId, form.amount.negate(), authenticatedUser)
        accountRepository.updateBalance(destinationAccountId, conversion.convertedAmount, authenticatedUser)

        log.info("Updated transfer in place groupId={} userId={}", groupId, authenticatedUser.id)
        return legs
    }

    override fun fetchTransfer(transactionId: UUID, authenticatedUser: UserDTO): TransferDetailsDTO {
        val transaction = transactionRepository.fetchTransactionById(transactionId, authenticatedUser)
        val groupId = transaction.transferGroupId
            ?: throw LocalizedException.BadRequest("error.transfer.notATransfer")
        val legs = transactionRepository.fetchTransferLegs(groupId, authenticatedUser)
        val sourceLeg = legs.firstOrNull { it.type == CategoryType.EXPENSE }
            ?: throw LocalizedException.BadRequest("error.transfer.legNotFound")
        val destinationLeg = legs.firstOrNull { it.type == CategoryType.INCOME }
            ?: throw LocalizedException.BadRequest("error.transfer.legNotFound")
        val sourceTx = transactionRepository.fetchTransactionById(sourceLeg.id, authenticatedUser)
        return TransferDetailsDTO(
            transferGroupId = groupId,
            sourceAccountId = sourceLeg.accountId,
            destinationAccountId = destinationLeg.accountId,
            amount = sourceLeg.amount,
            description = sourceTx.description,
            transactionDate = sourceTx.transactionDate ?: LocalDate.now(),
        )
    }

    @Transactional
    override fun bulkDelete(accountId: UUID, ids: List<UUID>, authenticatedUser: UserDTO): Int {
        if (ids.isEmpty()) return 0
        val selected = transactionRepository.fetchTransactionsByIds(accountId, ids, authenticatedUser)
        if (selected.isEmpty()) return 0

        val normalTxns = selected.filter { it.transferGroupId == null }
        val transferGroupIds = selected.mapNotNull { it.transferGroupId }.distinct()
        val transferLegs = transferGroupIds.flatMap { transactionRepository.fetchTransferLegs(it, authenticatedUser) }

        val allIds = (normalTxns.mapNotNull { it.id } + transferLegs.map { it.id }).distinct()
        val deleted = transactionRepository.deleteTransactionsByIds(allIds, authenticatedUser)
        if (deleted == 0) return 0

        val perAccount = mutableMapOf<UUID, BigDecimal>()
        normalTxns.forEach { tx ->
            perAccount.merge(accountId, calculateAdjustment(tx.type, tx.amount)) { a, b -> a + b }
        }
        transferLegs.forEach { leg ->
            perAccount.merge(leg.accountId, calculateAdjustment(leg.type, leg.amount)) { a, b -> a + b }
        }
        perAccount.forEach { (acc, net) ->
            if (net.signum() != 0) accountRepository.updateBalance(acc, net.negate(), authenticatedUser)
        }

        checkBudgetAlerts(authenticatedUser, normalTxns.mapNotNull { it.category.id }.distinct())
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
        val classification = categoriesRepository.fetchCategoryClassifications(authenticatedUser, listOf(categoryId))[categoryId]
            ?: throw categoryNotFound(categoryId)
        if (classification.isManaged) throw LocalizedException.BadRequest("error.category.managedAssign")
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

    override fun aggregateByDay(
        accountId: UUID, authenticatedUser: UserDTO, days: Int
    ): List<DailyAggregateDTO> =
        transactionRepository.aggregateByDay(accountId, authenticatedUser, days)

    @Transactional
    override fun importBatch(
        accountId: UUID,
        request: ImportTransactionsRequest,
        authenticatedUser: UserDTO,
    ): ImportResult {
        val rows = request.rows
        if (rows.isEmpty()) return ImportResult(0, 0)

        val account = accountRepository.fetchAccountById(accountId, authenticatedUser)
        val classifications = categoriesRepository.fetchCategoryClassifications(
            authenticatedUser, rows.mapTo(HashSet()) { it.categoryId }
        )
        // Apply the user's categorization rules to file imports too — but only to rows left on the
        // uncategorized fallback, so an explicit category choice is never overridden. Mirrors the
        // provider-sync path (importProviderTransactions) for consistent behaviour across channels.
        val fallbackCategoryId = categoriesRepository.fetchSystemCategoryByKey(UNCATEGORIZED_SYSTEM_KEY)?.id
        val rules = if (fallbackCategoryId != null) categorizationService.list(authenticatedUser) else emptyList()
        val resolvedRows = rows.map { row ->
            val classification = classifications[row.categoryId] ?: throw categoryNotFound(row.categoryId)
            val type = resolveType(classification, row.type)
            val categoryId = if (fallbackCategoryId != null && row.categoryId == fallbackCategoryId) {
                rules.firstMatch(row.description, type)?.category?.id ?: row.categoryId
            } else {
                row.categoryId
            }
            row.copy(categoryId = categoryId, type = type)
        }

        val conversions = convertRows(resolvedRows, account.currency)
        val hashes = resolvedRows.map { rowHash(accountId, it) }
        val outcome = transactionRepository.importBatch(accountId, resolvedRows, conversions, hashes, authenticatedUser)

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
    override fun importProviderTransactions(
        accountId: UUID,
        providerConnectionId: UUID,
        transactions: List<ImportedTransactionDTO>,
        authenticatedUser: UserDTO,
    ): ImportResult {
        if (transactions.isEmpty()) return ImportResult(0, 0)

        val fallbackCategoryId = categoriesRepository.fetchSystemCategoryByKey(UNCATEGORIZED_SYSTEM_KEY)?.id
        if (fallbackCategoryId == null) {
            log.error("Cannot persist synced transactions: '{}' system category is missing", UNCATEGORIZED_SYSTEM_KEY)
            return ImportResult(0, 0)
        }

        val account = accountRepository.fetchAccountById(accountId, authenticatedUser)
        val rules = categorizationService.list(authenticatedUser)
        val priced = transactions.filter { it.amount.signum() != 0 }
        val rows = priced.map { tx ->
            val type = if (tx.amount.signum() < 0) CategoryType.EXPENSE else CategoryType.INCOME
            val description = tx.description.ifBlank { tx.counterparty ?: "Imported transaction" }.take(255)
            ImportTransactionRow(
                amount = tx.amount.abs(),
                categoryId = rules.firstMatch(description, type)?.category?.id ?: fallbackCategoryId,
                description = description,
                transactionDate = tx.occurredAt.toLocalDate(),
                type = type,
                currency = parseProviderCurrency(tx.currency),
            )
        }
        val conversions = convertRows(rows, account.currency)
        val hashes = priced.map { providerRowHash(providerConnectionId, it.externalId) }

        val outcome =
            transactionRepository.importBatch(accountId, rows, conversions, hashes, authenticatedUser, providerConnectionId)
        if (outcome.netBalanceAdjustment.signum() != 0) {
            accountRepository.updateBalance(accountId, outcome.netBalanceAdjustment, authenticatedUser)
            checkBudgetAlerts(authenticatedUser, rows.map { it.categoryId }.distinct())
        }

        log.info(
            "Imported provider batch accountId={} connectionId={} userId={} fetched={} inserted={} skippedDuplicates={}",
            accountId, providerConnectionId, authenticatedUser.id, transactions.size,
            outcome.insertedCount, priced.size - outcome.insertedCount,
        )
        return ImportResult(
            imported = outcome.insertedCount,
            skippedDuplicates = priced.size - outcome.insertedCount,
        )
    }

    private fun providerRowHash(providerConnectionId: UUID, externalId: String): String {
        val payload = "provider:$providerConnectionId:$externalId"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
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
            throw LocalizedException.BadRequest("error.balance.unchanged")
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

    override fun previewConversion(
        accountId: UUID,
        amount: BigDecimal,
        currency: Currency,
        date: LocalDate,
        authenticatedUser: UserDTO,
    ): ConversionResult {
        val account = accountRepository.fetchAccountById(accountId, authenticatedUser)
        return currencyConversionService.convert(amount, currency, account.currency, date)
    }

    private fun rowHash(accountId: UUID, row: ImportTransactionRow): String {
        val currencyPart = row.currency?.let { "|${it.name}" } ?: ""
        val typePart = row.type?.let { "|${it.name}" } ?: ""
        val payload =
            "$accountId|${row.transactionDate}|${row.amount.toPlainString()}|${row.description}|${row.categoryId}$typePart$currencyPart"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun convertRows(rows: List<ImportTransactionRow>, accountCurrency: Currency): List<ConversionResult> =
        rows.map { row ->
            currencyConversionService.convert(
                row.amount, row.currency ?: accountCurrency, accountCurrency, row.transactionDate
            )
        }

    private fun parseProviderCurrency(code: String?): Currency? =
        code?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { Currency.valueOf(raw.uppercase()) }.getOrElse {
                log.warn("Unknown provider currency '{}'; defaulting to account currency", raw)
                null
            }
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

    private fun categoryNotFound(categoryId: Long): LocalizedException {
        log.warn("Category {} not found or not accessible", categoryId)
        return LocalizedException.BadRequest("error.category.notAccessible")
    }

    private fun calculateAdjustment(type: CategoryType?, amount: BigDecimal?): BigDecimal {
        val value = amount ?: BigDecimal.ZERO
        return when (type) {
            CategoryType.INCOME -> value
            CategoryType.EXPENSE -> value.negate()
            else -> throw IllegalStateException("Invalid transaction type: $type")
        }
    }
}
