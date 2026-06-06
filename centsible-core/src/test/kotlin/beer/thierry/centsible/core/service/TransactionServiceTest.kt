package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.transaction.SetBalanceForm
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransferForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.CategoryClassification
import beer.thierry.centsible.api.repository.TransferLeg
import beer.thierry.centsible.api.repository.IAttachmentRepository
import beer.thierry.centsible.api.repository.IBudgetAccountHistoryRepository
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.services.categorization.ICategorizationService
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.api.services.notifications.INotificationService
import beer.thierry.centsible.core.services.transactions.TransactionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.lenient
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class TransactionServiceTest {

    // Helpers — Mockito's `any()` returns null which Kotlin's non-null types reject;
    // these reify the type so the call sites read naturally.
    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()
    private fun <T> eqArg(value: T): T = org.mockito.ArgumentMatchers.eq(value) ?: value

    @Mock
    private lateinit var transactionRepository: ITransactionRepository

    @Mock
    private lateinit var accountRepository: IBudgetAccountsRepository

    @Mock
    private lateinit var accountHistoryRepository: IBudgetAccountHistoryRepository

    @Mock
    private lateinit var notificationService: INotificationService

    @Mock
    private lateinit var categoriesRepository: ICategoriesRepository

    @Mock
    private lateinit var attachmentRepository: IAttachmentRepository

    @Mock
    private lateinit var categorizationService: ICategorizationService

    @Mock
    private lateinit var currencyConversionService: ICurrencyConversionService

    @InjectMocks
    private lateinit var service: TransactionService

    private val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
    private val accountId = UUID.randomUUID()
    private val eurAccount = BudgetAccountDTO(accountId, "Account", BigDecimal("0.00"), BigDecimal("0.00"), Currency.EUR)
    private val expenseCategory = CategoryDTO(1L, "Category", "icon", "#FF0000", CategoryType.EXPENSE)
    private val incomeCategory = CategoryDTO(2L, "Income Category", "icon", "#00FF00", CategoryType.INCOME)

    // By default the account is EUR and conversions are same-currency no-ops echoing the amount, so the
    // balance-math assertions below are unaffected. lenient() because delete/bulk tests never convert.
    @BeforeEach
    fun stubAccountAndConversion() {
        lenient().`when`(accountRepository.fetchAccountById(accountId, user)).thenReturn(eurAccount)
        // doAnswer (not when/thenAnswer) so a per-test re-stub of convert() doesn't re-trigger this answer.
        lenient().doAnswer { inv ->
            val amount = inv.getArgument<BigDecimal>(0)
            val from = inv.getArgument<Currency>(1)
            val to = inv.getArgument<Currency>(2)
            val date = inv.getArgument<LocalDate>(3)
            ConversionResult(amount, amount, from, to, BigDecimal.ONE, date, from == to)
        }.`when`(currencyConversionService).convert(anyArg(), anyArg(), anyArg(), anyArg())
    }

    private fun stubCategoryType(categoryId: Long, type: CategoryType, isManaged: Boolean = false) {
        `when`(categoriesRepository.fetchCategoryClassifications(user, listOf(categoryId)))
            .thenReturn(mapOf(categoryId to CategoryClassification(type, isManaged)))
    }

    @Test
    fun `createTransaction should update balance for INCOME`() {
        val form = TransactionForm(BigDecimal("50.00"), 2L, "Income", LocalDate.now())
        val transaction = TransactionDTO(
            id = UUID.randomUUID(),
            category = incomeCategory,
            type = CategoryType.INCOME,
            amount = BigDecimal("50.00"),
            description = "Income",
            transactionDate = LocalDate.now(),
        )
        stubCategoryType(2L, CategoryType.INCOME)
        `when`(
            transactionRepository.createTransaction(
                eqArg(accountId), eqArg(form.copy(type = CategoryType.INCOME)), anyArg(), eqArg(user)
            )
        ).thenReturn(transaction)

        val result = service.createTransaction(accountId, form, user)

        assertEquals(transaction, result)
        verify(accountRepository).updateBalance(accountId, BigDecimal("50.00"), user)
    }

    @Test
    fun `createTransaction should update balance for EXPENSE`() {
        val form = TransactionForm(BigDecimal("30.00"), 1L, "Expense", LocalDate.now())
        val transaction = TransactionDTO(
            id = UUID.randomUUID(),
            category = expenseCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("30.00"),
            description = "Expense",
            transactionDate = LocalDate.now(),
        )
        stubCategoryType(1L, CategoryType.EXPENSE)
        `when`(
            transactionRepository.createTransaction(
                eqArg(accountId), eqArg(form.copy(type = CategoryType.EXPENSE)), anyArg(), eqArg(user)
            )
        ).thenReturn(transaction)

        val result = service.createTransaction(accountId, form, user)

        assertEquals(transaction, result)
        verify(accountRepository).updateBalance(accountId, BigDecimal("-30.00"), user)
    }

    @Test
    fun `createTransaction converts a foreign-currency amount and adjusts balance by the converted value`() {
        val form = TransactionForm(BigDecimal("10.00"), 1L, "Coffee", LocalDate.now(), currency = Currency.USD)
        val conversion = ConversionResult(
            convertedAmount = BigDecimal("9.07"),
            originalAmount = BigDecimal("10.00"),
            originalCurrency = Currency.USD,
            accountCurrency = Currency.EUR,
            rate = BigDecimal("0.907"),
            rateDate = LocalDate.now(),
            sameCurrency = false,
        )
        val stored = TransactionDTO(
            id = UUID.randomUUID(),
            category = expenseCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("9.07"),
            description = "Coffee",
            transactionDate = LocalDate.now(),
            originalAmount = BigDecimal("10.00"),
            originalCurrency = Currency.USD,
            exchangeRate = BigDecimal("0.907"),
        )
        stubCategoryType(1L, CategoryType.EXPENSE)
        doReturn(conversion).`when`(currencyConversionService)
            .convert(eqArg(BigDecimal("10.00")), eqArg(Currency.USD), eqArg(Currency.EUR), anyArg())
        `when`(transactionRepository.createTransaction(eqArg(accountId), anyArg(), eqArg(conversion), eqArg(user)))
            .thenReturn(stored)

        service.createTransaction(accountId, form, user)

        // Balance moves by the converted EUR amount, never the original 10 USD.
        verify(accountRepository).updateBalance(accountId, BigDecimal("-9.07"), user)
    }

    @Test
    fun `createTransaction with explicit type override beats category default`() {
        // INCOME-typed category but user marks the transaction EXPENSE (e.g. a refund-style scenario).
        val form = TransactionForm(BigDecimal("75.00"), 2L, "Override", LocalDate.now(), type = CategoryType.EXPENSE)
        val transaction = TransactionDTO(
            id = UUID.randomUUID(),
            category = incomeCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("75.00"),
            description = "Override",
            transactionDate = LocalDate.now(),
        )
        stubCategoryType(2L, CategoryType.INCOME)
        `when`(transactionRepository.createTransaction(eqArg(accountId), eqArg(form), anyArg(), eqArg(user)))
            .thenReturn(transaction)

        service.createTransaction(accountId, form, user)

        // Service must not mutate the caller's form.
        assertEquals(CategoryType.EXPENSE, form.type)
        verify(accountRepository).updateBalance(accountId, BigDecimal("-75.00"), user)
    }

    @Test
    fun `createTransaction with managed category ignores user override`() {
        val managedCategoryId = 99L
        val form = TransactionForm(BigDecimal("200.00"), managedCategoryId, "Lend", LocalDate.now(), type = CategoryType.INCOME)
        val transaction = TransactionDTO(
            id = UUID.randomUUID(),
            category = expenseCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("200.00"),
            description = "Lend",
            transactionDate = LocalDate.now(),
        )
        stubCategoryType(managedCategoryId, CategoryType.EXPENSE, isManaged = true)
        `when`(
            transactionRepository.createTransaction(
                eqArg(accountId), eqArg(form.copy(type = CategoryType.EXPENSE)), anyArg(), eqArg(user)
            )
        ).thenReturn(transaction)

        service.createTransaction(accountId, form, user)

        // Caller's form must not be mutated; the managed-type rule lives in the resolved copy.
        assertEquals(CategoryType.INCOME, form.type)
        verify(accountRepository).updateBalance(accountId, BigDecimal("-200.00"), user)
    }

    @Test
    fun `updateTransaction should reverse old and apply new transaction`() {
        val transactionId = UUID.randomUUID()
        val form = TransactionForm(BigDecimal("100.00"), 2L, "New Income", LocalDate.now())

        val oldTransaction = TransactionDTO(
            id = transactionId,
            category = expenseCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("50.00"),
            description = "Old Expense",
            transactionDate = LocalDate.now(),
        )
        val updatedTransaction = TransactionDTO(
            id = transactionId,
            category = incomeCategory,
            type = CategoryType.INCOME,
            amount = BigDecimal("100.00"),
            description = "New Income",
            transactionDate = LocalDate.now(),
        )

        stubCategoryType(2L, CategoryType.INCOME)
        `when`(transactionRepository.fetchTransactionById(transactionId, user)).thenReturn(oldTransaction)
        `when`(
            transactionRepository.updateTransaction(
                eqArg(transactionId), eqArg(accountId), eqArg(form.copy(type = CategoryType.INCOME)), anyArg(), eqArg(user)
            )
        ).thenReturn(updatedTransaction)

        val result = service.updateTransaction(transactionId, accountId, form, user)

        assertEquals(updatedTransaction, result)
        // reverse old expense (+50) + apply new income (+100) = +150
        verify(accountRepository).updateBalance(accountId, BigDecimal("150.00"), user)
    }

    @Test
    fun `deleteTransaction should reverse transaction`() {
        val transactionId = UUID.randomUUID()
        val transaction = TransactionDTO(
            id = transactionId,
            category = incomeCategory,
            type = CategoryType.INCOME,
            amount = BigDecimal("40.00"),
            description = "Income",
            transactionDate = LocalDate.now(),
        )

        `when`(transactionRepository.deleteTransaction(transactionId, accountId, user)).thenReturn(transaction)

        val result = service.deleteTransaction(transactionId, accountId, user)

        assertEquals(transaction, result)
        verify(accountRepository).updateBalance(accountId, BigDecimal("-40.00"), user)
    }

    @Test
    fun `deleteTransaction should reverse EXPENSE`() {
        val transactionId = UUID.randomUUID()
        val transaction = TransactionDTO(
            id = transactionId,
            category = expenseCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("25.00"),
            description = "Expense",
            transactionDate = LocalDate.now(),
        )

        `when`(transactionRepository.deleteTransaction(transactionId, accountId, user)).thenReturn(transaction)

        val result = service.deleteTransaction(transactionId, accountId, user)

        assertEquals(transaction, result)
        verify(accountRepository).updateBalance(accountId, BigDecimal("25.00"), user)
    }

    @Test
    fun `deleteTransaction with a mismatched account is rejected and never touches the balance`() {
        val transactionId = UUID.randomUUID()
        // The guarded repository delete finds 0 rows for this (transaction, account) pair and throws,
        // so the service must abort before adjusting any balance — the core of the fixed bug.
        `when`(transactionRepository.deleteTransaction(transactionId, accountId, user))
            .thenThrow(IllegalArgumentException("Transaction not found or not owned by user"))

        assertThrows(IllegalArgumentException::class.java) {
            service.deleteTransaction(transactionId, accountId, user)
        }

        verify(accountRepository, never()).updateBalance(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `updateTransaction should reverse old INCOME and apply new EXPENSE`() {
        val transactionId = UUID.randomUUID()
        val form = TransactionForm(BigDecimal("80.00"), 1L, "New Expense", LocalDate.now())

        val oldTransaction = TransactionDTO(
            id = transactionId,
            category = incomeCategory,
            type = CategoryType.INCOME,
            amount = BigDecimal("120.00"),
            description = "Old Income",
            transactionDate = LocalDate.now(),
        )
        val updatedTransaction = TransactionDTO(
            id = transactionId,
            category = expenseCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("80.00"),
            description = "New Expense",
            transactionDate = LocalDate.now(),
        )

        stubCategoryType(1L, CategoryType.EXPENSE)
        `when`(transactionRepository.fetchTransactionById(transactionId, user)).thenReturn(oldTransaction)
        `when`(
            transactionRepository.updateTransaction(
                eqArg(transactionId), eqArg(accountId), eqArg(form.copy(type = CategoryType.EXPENSE)), anyArg(), eqArg(user)
            )
        ).thenReturn(updatedTransaction)

        val result = service.updateTransaction(transactionId, accountId, form, user)

        assertEquals(updatedTransaction, result)
        // reverse old income (-120) + apply new expense (-80) = -200
        verify(accountRepository).updateBalance(accountId, BigDecimal("-200.00"), user)
    }

    @Test
    fun `bulkUpdateCategory does not touch balance`() {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID())
        val newCategoryId = 7L

        val oldTransactions = listOf(
            TransactionDTO(
                id = ids[0],
                category = expenseCategory,
                type = CategoryType.EXPENSE,
                amount = BigDecimal("12.00"),
            ),
            TransactionDTO(
                id = ids[1],
                category = expenseCategory,
                type = CategoryType.EXPENSE,
                amount = BigDecimal("34.00"),
            ),
        )

        `when`(transactionRepository.fetchTransactionsByIds(accountId, ids, user)).thenReturn(oldTransactions)
        `when`(
            transactionRepository.updateCategoryForTransactions(accountId, ids, newCategoryId, user)
        ).thenReturn(2)

        val updated = service.bulkUpdateCategory(accountId, ids, newCategoryId, user)

        assertEquals(2, updated)
        verify(accountRepository, never()).updateBalance(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `createBalanceAdjustment with newBalance greater than current creates INCOME transaction`() {
        val form = SetBalanceForm(
            newBalance = BigDecimal("250.00"),
            categoryId = 2L,
            description = "Top-up",
            transactionDate = LocalDate.now(),
        )
        val account = BudgetAccountDTO(accountId, "Account", BigDecimal("100.00"), BigDecimal("0.00"), Currency.EUR)
        val createdTx = TransactionDTO(
            id = UUID.randomUUID(),
            category = incomeCategory,
            type = CategoryType.INCOME,
            amount = BigDecimal("150.00"),
            description = "Top-up",
            transactionDate = LocalDate.now(),
        )

        `when`(accountRepository.fetchAccountById(accountId, user)).thenReturn(account)
        stubCategoryType(2L, CategoryType.INCOME)
        `when`(
            transactionRepository.createTransaction(eqArg(accountId), anyArg(), anyArg(), eqArg(user))
        ).thenReturn(createdTx)

        val result = service.createBalanceAdjustment(accountId, form, user)

        assertEquals(createdTx, result)
        verify(accountRepository).updateBalance(accountId, BigDecimal("150.00"), user)
    }

    @Test
    fun `createBalanceAdjustment with newBalance less than current creates EXPENSE transaction`() {
        val form = SetBalanceForm(
            newBalance = BigDecimal("60.00"),
            categoryId = 1L,
            description = "Mark down",
            transactionDate = LocalDate.now(),
        )
        val account = BudgetAccountDTO(accountId, "Account", BigDecimal("100.00"), BigDecimal("0.00"), Currency.EUR)
        val createdTx = TransactionDTO(
            id = UUID.randomUUID(),
            category = expenseCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("40.00"),
            description = "Mark down",
            transactionDate = LocalDate.now(),
        )

        `when`(accountRepository.fetchAccountById(accountId, user)).thenReturn(account)
        stubCategoryType(1L, CategoryType.EXPENSE)
        `when`(
            transactionRepository.createTransaction(eqArg(accountId), anyArg(), anyArg(), eqArg(user))
        ).thenReturn(createdTx)

        val result = service.createBalanceAdjustment(accountId, form, user)

        assertEquals(createdTx, result)
        verify(accountRepository).updateBalance(accountId, BigDecimal("-40.00"), user)
    }

    @Test
    fun `createTransfer moves money out of source and into destination`() {
        val destAccountId = UUID.randomUUID()
        val destAccount = BudgetAccountDTO(destAccountId, "Savings", BigDecimal("0.00"), BigDecimal("0.00"), Currency.EUR)
        lenient().`when`(accountRepository.fetchAccountById(destAccountId, user)).thenReturn(destAccount)
        val form = TransferForm(BigDecimal("100.00"), destAccountId, "Move", LocalDate.now())
        val legs = listOf(
            TransactionDTO(id = UUID.randomUUID(), type = CategoryType.EXPENSE, amount = BigDecimal("100.00")),
            TransactionDTO(id = UUID.randomUUID(), type = CategoryType.INCOME, amount = BigDecimal("100.00")),
        )
        `when`(transactionRepository.insertTransfer(eqArg(accountId), eqArg(destAccountId), anyArg(), anyArg(), eqArg(user)))
            .thenReturn(legs)

        val result = service.createTransfer(accountId, form, user)

        assertEquals(2, result.size)
        verify(accountRepository).updateBalance(accountId, BigDecimal("-100.00"), user)
        verify(accountRepository).updateBalance(destAccountId, BigDecimal("100.00"), user)
    }

    @Test
    fun `createTransfer converts the destination leg across currencies`() {
        val destAccountId = UUID.randomUUID()
        val usdAccount = BudgetAccountDTO(destAccountId, "USD", BigDecimal("0.00"), BigDecimal("0.00"), Currency.USD)
        `when`(accountRepository.fetchAccountById(destAccountId, user)).thenReturn(usdAccount)
        doReturn(
            ConversionResult(BigDecimal("110.00"), BigDecimal("100.00"), Currency.EUR, Currency.USD, BigDecimal("1.10"), LocalDate.now(), false)
        ).`when`(currencyConversionService).convert(eqArg(BigDecimal("100.00")), eqArg(Currency.EUR), eqArg(Currency.USD), anyArg())
        val form = TransferForm(BigDecimal("100.00"), destAccountId, "FX move", LocalDate.now())
        `when`(transactionRepository.insertTransfer(eqArg(accountId), eqArg(destAccountId), anyArg(), anyArg(), eqArg(user)))
            .thenReturn(listOf(TransactionDTO(id = UUID.randomUUID()), TransactionDTO(id = UUID.randomUUID())))

        service.createTransfer(accountId, form, user)

        verify(accountRepository).updateBalance(accountId, BigDecimal("-100.00"), user)
        verify(accountRepository).updateBalance(destAccountId, BigDecimal("110.00"), user)
    }

    @Test
    fun `createTransfer rejects identical source and destination`() {
        val form = TransferForm(BigDecimal("10.00"), accountId, "Self", LocalDate.now())

        assertThrows(IllegalArgumentException::class.java) {
            service.createTransfer(accountId, form, user)
        }

        verify(accountRepository, never()).updateBalance(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `deleteTransaction removes both transfer legs and reverses both balances`() {
        val groupId = UUID.randomUUID()
        val legTxId = UUID.randomUUID()
        val destAccountId = UUID.randomUUID()
        val deletedLeg = TransactionDTO(id = legTxId, type = CategoryType.EXPENSE, amount = BigDecimal("100.00"), transferGroupId = groupId)
        `when`(transactionRepository.deleteTransaction(legTxId, accountId, user)).thenReturn(deletedLeg)
        `when`(transactionRepository.fetchTransferLegs(groupId, user)).thenReturn(
            listOf(TransferLeg(UUID.randomUUID(), destAccountId, CategoryType.INCOME, BigDecimal("110.00")))
        )
        `when`(transactionRepository.deleteTransactionsByIds(anyArg(), eqArg(user))).thenReturn(1)

        service.deleteTransaction(legTxId, accountId, user)

        verify(accountRepository).updateBalance(accountId, BigDecimal("100.00"), user)
        verify(accountRepository).updateBalance(destAccountId, BigDecimal("-110.00"), user)
    }

    @Test
    fun `createBalanceAdjustment with newBalance equal to current throws`() {
        val form = SetBalanceForm(
            newBalance = BigDecimal("100.00"),
            categoryId = 1L,
            description = "No change",
            transactionDate = LocalDate.now(),
        )
        val account = BudgetAccountDTO(accountId, "Account", BigDecimal("100.00"), BigDecimal("0.00"), Currency.EUR)

        `when`(accountRepository.fetchAccountById(accountId, user)).thenReturn(account)

        assertThrows(IllegalArgumentException::class.java) {
            service.createBalanceAdjustment(accountId, form, user)
        }
    }
}
