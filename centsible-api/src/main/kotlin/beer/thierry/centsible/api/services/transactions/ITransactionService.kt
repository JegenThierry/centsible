package beer.thierry.centsible.api.services.transactions

import beer.thierry.centsible.api.model.DEFAULT_PAGE_SIZE
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.integrations.ImportedTransactionDTO
import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.DailyAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionsRequest
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.SetBalanceForm
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransferDetailsDTO
import beer.thierry.centsible.api.model.transaction.TransferForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

interface ITransactionService {
    fun fetchTransactions(
        accountId: UUID,
        authenticatedUser: UserDTO,
        page: Int,
        size: Int = DEFAULT_PAGE_SIZE,
        filters: TransactionFilters = TransactionFilters(),
    ): List<TransactionDTO>

    fun createTransaction(accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO): TransactionDTO
    fun updateTransaction(
        transactionId: UUID,
        accountId: UUID,
        transactionForm: TransactionForm,
        authenticatedUser: UserDTO
    ): TransactionDTO

    fun deleteTransaction(transactionId: UUID, accountId: UUID, authenticatedUser: UserDTO): TransactionDTO

    fun createTransfer(sourceAccountId: UUID, form: TransferForm, authenticatedUser: UserDTO): List<TransactionDTO>

    fun updateTransfer(
        transactionId: UUID,
        sourceAccountId: UUID,
        form: TransferForm,
        authenticatedUser: UserDTO,
    ): List<TransactionDTO>

    fun fetchTransfer(transactionId: UUID, authenticatedUser: UserDTO): TransferDetailsDTO

    fun bulkDelete(accountId: UUID, ids: List<UUID>, authenticatedUser: UserDTO): Int

    /** Reassigns [ids] to [categoryId] without touching balances; rejects system-managed target categories. Returns rows updated. */
    fun bulkUpdateCategory(accountId: UUID, ids: List<UUID>, categoryId: Long, authenticatedUser: UserDTO): Int

    /** Adds [tagIds] to each of [ids] (user-owned) without touching balances. Returns tag links created. */
    fun bulkAddTags(ids: List<UUID>, tagIds: List<Long>, authenticatedUser: UserDTO): Int

    /** Removes [tagIds] from each of [ids] (user-owned) without touching balances. Returns tag links removed. */
    fun bulkRemoveTags(ids: List<UUID>, tagIds: List<Long>, authenticatedUser: UserDTO): Int

    fun aggregateByCategory(
        accountId: UUID,
        authenticatedUser: UserDTO,
        from: LocalDate,
        to: LocalDate,
    ): List<CategoryAggregateDTO>

    fun aggregateByMonth(
        accountId: UUID,
        authenticatedUser: UserDTO,
        months: Int = 6,
    ): List<MonthlyAggregateDTO>

    fun aggregateByDay(
        accountId: UUID,
        authenticatedUser: UserDTO,
        days: Int = 365,
    ): List<DailyAggregateDTO>

    fun importBatch(
        accountId: UUID,
        request: ImportTransactionsRequest,
        authenticatedUser: UserDTO,
    ): ImportResult

    /**
     * Persists transactions fetched from a provider sync into [accountId], deduping by the
     * provider's stable external id and stamping [providerConnectionId] for provenance. Rows land
     * in the "Uncategorized" fallback category until a rule or the user assigns one. Returns how
     * many rows were inserted versus skipped as duplicates.
     */
    fun importProviderTransactions(
        accountId: UUID,
        providerConnectionId: UUID,
        transactions: List<ImportedTransactionDTO>,
        authenticatedUser: UserDTO,
    ): ImportResult

    /** Records an income/expense adjustment transaction bringing the account to the form's target balance; rejects a no-op delta. */
    fun createBalanceAdjustment(
        accountId: UUID,
        form: SetBalanceForm,
        authenticatedUser: UserDTO,
    ): TransactionDTO

    /** Computes how [amount] in [currency] would convert into the account's currency on [date], without persisting anything. */
    fun previewConversion(
        accountId: UUID,
        amount: BigDecimal,
        currency: Currency,
        date: LocalDate,
        authenticatedUser: UserDTO,
    ): ConversionResult
}
