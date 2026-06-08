package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.DailyAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransactionSplitDTO
import beer.thierry.centsible.api.model.transaction.TransactionSplitForm
import beer.thierry.centsible.api.model.transaction.TransferForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

interface ITransactionRepository {
    fun fetchTransactions(
        accountId: UUID,
        authenticatedUser: UserDTO,
        page: Int,
        pageSize: Int,
        filters: TransactionFilters = TransactionFilters(),
    ): List<TransactionDTO>
    fun fetchTransactionById(transactionId: UUID, authenticatedUser: UserDTO): TransactionDTO
    fun createTransaction(
        accountId: UUID,
        transactionForm: TransactionForm,
        conversion: ConversionResult,
        authenticatedUser: UserDTO,
    ): TransactionDTO
    fun updateTransaction(
        transactionId: UUID,
        accountId: UUID,
        transactionForm: TransactionForm,
        conversion: ConversionResult,
        authenticatedUser: UserDTO,
    ): TransactionDTO

    /** Deletes transaction [transactionId] belonging to [accountId] owned by [authenticatedUser]; throws if none matches. */
    fun deleteTransaction(transactionId: UUID, accountId: UUID, authenticatedUser: UserDTO): TransactionDTO

    fun insertTransfer(
        sourceAccountId: UUID,
        destinationAccountId: UUID,
        form: TransferForm,
        conversion: ConversionResult,
        authenticatedUser: UserDTO,
    ): List<TransactionDTO>

    fun fetchTransferLegs(transferGroupId: UUID, authenticatedUser: UserDTO): List<TransferLeg>

    /**
     * Updates both legs of an existing transfer in place — preserving their ids, [TransferLeg]'s
     * group, attachments and createdAt — rather than delete-and-recreate. The source (EXPENSE) leg
     * is set to [sourceAccountId]/[TransferForm.amount]; the destination (INCOME) leg to
     * [destinationAccountId]/[ConversionResult.convertedAmount]. Throws if either leg is missing.
     */
    fun updateTransfer(
        sourceLegId: UUID,
        destinationLegId: UUID,
        sourceAccountId: UUID,
        destinationAccountId: UUID,
        form: TransferForm,
        conversion: ConversionResult,
        authenticatedUser: UserDTO,
    ): List<TransactionDTO>

    /** Deletes [authenticatedUser]'s transactions matching [ids] across all accounts (no account scope). Returns count deleted. */
    fun deleteTransactionsByIds(ids: List<UUID>, authenticatedUser: UserDTO): Int

    /** Returns the transactions matching [ids] that belong to [accountId] owned by [authenticatedUser]. */
    fun fetchTransactionsByIds(
        accountId: UUID,
        ids: List<UUID>,
        authenticatedUser: UserDTO,
    ): List<TransactionDTO>

    /** Deletes transactions in [ids] belonging to [accountId]. Returns count actually deleted. */
    fun deleteTransactions(
        accountId: UUID,
        ids: List<UUID>,
        authenticatedUser: UserDTO,
    ): Int

    /** Reassigns the category for transactions in [ids] belonging to [accountId]. Returns count updated. */
    fun updateCategoryForTransactions(
        accountId: UUID,
        ids: List<UUID>,
        categoryId: Long,
        authenticatedUser: UserDTO,
    ): Int

    /** Sums EXPENSE transactions per category for [accountId] within [from]..[to] (inclusive). */
    fun aggregateByCategory(
        accountId: UUID,
        authenticatedUser: UserDTO,
        from: LocalDate,
        to: LocalDate,
    ): List<CategoryAggregateDTO>

    /** Returns income and expense totals per month for the last [months] calendar months (most-recent last). */
    fun aggregateByMonth(
        accountId: UUID,
        authenticatedUser: UserDTO,
        months: Int,
    ): List<MonthlyAggregateDTO>

    /** Returns income and expense totals per day for the last [days] calendar days (most-recent last). */
    fun aggregateByDay(
        accountId: UUID,
        authenticatedUser: UserDTO,
        days: Int,
    ): List<DailyAggregateDTO>

    fun importBatch(
        accountId: UUID,
        rows: List<ImportTransactionRow>,
        conversions: List<ConversionResult>,
        hashes: List<String>,
        authenticatedUser: UserDTO,
        providerConnectionId: UUID? = null,
    ): BatchImportOutcome

    /** Lightweight projection of the user's non-transfer, non-split transactions for in-memory rule evaluation. */
    fun fetchForRuleEvaluation(authenticatedUser: UserDTO): List<RuleCandidateTransaction>

    /** Sets the category of the given user-owned transactions (across accounts). Returns rows updated. */
    fun setCategoryForTransactions(authenticatedUser: UserDTO, ids: List<UUID>, categoryId: Long): Int

    /** Returns the splits of each given user-owned transaction, keyed by transaction id (absent when none). */
    fun fetchSplitsByTransactionIds(
        authenticatedUser: UserDTO,
        transactionIds: List<UUID>,
    ): Map<UUID, List<TransactionSplitDTO>>

    /**
     * Replaces all splits of [transactionId] (owned by [authenticatedUser]) with [splits].
     * An empty list clears the splits, turning the transaction back into a simple single-category one.
     */
    fun replaceSplits(
        transactionId: UUID,
        splits: List<TransactionSplitForm>,
        authenticatedUser: UserDTO,
    )

    /** Deletes all splits of the given user-owned transactions. Returns the number of split rows removed. */
    fun deleteSplitsForTransactions(transactionIds: List<UUID>, authenticatedUser: UserDTO): Int
}

data class BatchImportOutcome(
    val insertedCount: Int,
    val netBalanceAdjustment: BigDecimal,
)

data class TransferLeg(
    val id: UUID,
    val accountId: UUID,
    val type: CategoryType,
    val amount: BigDecimal,
)

data class RuleCandidateTransaction(
    val id: UUID,
    val accountId: UUID,
    val description: String,
    val amount: BigDecimal,
    val type: CategoryType,
    val categoryId: Long,
    val isManagedCategory: Boolean,
)
