package beer.thierry.centsible.core.service

import org.springframework.transaction.support.TransactionTemplate
import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.rule.RuleActionDTO
import beer.thierry.centsible.api.model.rule.RuleActionType
import beer.thierry.centsible.api.model.rule.RuleConditionDTO
import beer.thierry.centsible.api.model.rule.RuleDTO
import beer.thierry.centsible.api.model.rule.RuleField
import beer.thierry.centsible.api.model.rule.RuleOperator
import beer.thierry.centsible.api.model.tag.TagDTO
import beer.thierry.centsible.api.model.transaction.SetBalanceForm
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransactionSplitDTO
import beer.thierry.centsible.api.model.transaction.TransactionSplitForm
import beer.thierry.centsible.api.model.transaction.TransferForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.CategoryClassification
import beer.thierry.centsible.api.repository.TransferLeg
import beer.thierry.centsible.api.repository.IAttachmentRepository
import beer.thierry.centsible.api.repository.IBudgetAccountHistoryRepository
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.ITagRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.services.rule.IRuleService
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
    private lateinit var tagRepository: ITagRepository

    @Mock
    private lateinit var ruleService: IRuleService

    @Mock
    private lateinit var currencyConversionService: ICurrencyConversionService

    @Mock
    private lateinit var transactionTemplate: TransactionTemplate

    @InjectMocks
    private lateinit var service: TransactionService

    private val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
    private val accountId = UUID.randomUUID()
    private val eurAccount = BudgetAccountDTO(accountId, "Account", BigDecimal("0.00"), BigDecimal("0.00"), Currency.EUR)
    private val expenseCategory = CategoryDTO(1L, "Category", "icon", "#FF0000", CategoryType.EXPENSE)
    private val incomeCategory = CategoryDTO(2L, "Income Category", "icon", "#00FF00", CategoryType.INCOME)

    @BeforeEach
    fun stubAccountAndConversion() {
        lenient().`when`(accountRepository.fetchAccountById(accountId, user)).thenReturn(eurAccount)
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
    fun `createTransaction applies rules when the category is the uncategorized fallback`() {
        val uncategorized = CategoryDTO(99L, "Uncategorized", "icon", "#999999", CategoryType.EXPENSE)
        val form = TransactionForm(BigDecimal("12.99"), 99L, "Netflix subscription", LocalDate.now())
        val rule = RuleDTO(
            id = UUID.randomUUID(),
            name = "netflix",
            matchAll = true,
            enabled = true,
            priority = 0,
            conditions = listOf(
                RuleConditionDTO(field = RuleField.DESCRIPTION, operator = RuleOperator.CONTAINS, value = "netflix")
            ),
            actions = listOf(
                RuleActionDTO(type = RuleActionType.SET_CATEGORY, category = expenseCategory),
                RuleActionDTO(type = RuleActionType.ADD_TAG, tag = TagDTO(id = 7L, name = "streaming")),
            ),
            createdAt = java.time.OffsetDateTime.now(),
            updatedAt = java.time.OffsetDateTime.now(),
        )
        val transaction = TransactionDTO(
            id = UUID.randomUUID(),
            category = expenseCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("12.99"),
            description = "Netflix subscription",
            transactionDate = LocalDate.now(),
        )
        stubCategoryType(99L, CategoryType.EXPENSE, isManaged = true)
        `when`(categoriesRepository.fetchSystemCategoryByKey("UNCATEGORIZED")).thenReturn(uncategorized)
        `when`(ruleService.list(user)).thenReturn(listOf(rule))
        `when`(
            transactionRepository.createTransaction(
                eqArg(accountId), eqArg(form.copy(type = CategoryType.EXPENSE, categoryId = 1L)), anyArg(), eqArg(user)
            )
        ).thenReturn(transaction)

        val result = service.createTransaction(accountId, form, user)

        assertEquals(transaction, result)
        verify(tagRepository).setTransactionTags(user, transaction.id!!, listOf(7L))
    }

    @Test
    fun `createTransaction never overrides an explicitly chosen category with rules`() {
        val uncategorized = CategoryDTO(99L, "Uncategorized", "icon", "#999999", CategoryType.EXPENSE)
        val form = TransactionForm(BigDecimal("12.99"), 1L, "Netflix subscription", LocalDate.now())
        val transaction = TransactionDTO(
            id = UUID.randomUUID(),
            category = expenseCategory,
            type = CategoryType.EXPENSE,
            amount = BigDecimal("12.99"),
            description = "Netflix subscription",
            transactionDate = LocalDate.now(),
        )
        stubCategoryType(1L, CategoryType.EXPENSE)
        `when`(categoriesRepository.fetchSystemCategoryByKey("UNCATEGORIZED")).thenReturn(uncategorized)
        `when`(
            transactionRepository.createTransaction(
                eqArg(accountId), eqArg(form.copy(type = CategoryType.EXPENSE)), anyArg(), eqArg(user)
            )
        ).thenReturn(transaction)

        val result = service.createTransaction(accountId, form, user)

        assertEquals(transaction, result)
        verify(ruleService, never()).list(anyArg())
        verify(tagRepository, never()).setTransactionTags(anyArg(), anyArg(), anyArg())
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

        verify(accountRepository).updateBalance(accountId, BigDecimal("-9.07"), user)
    }

    @Test
    fun `createTransaction with explicit type override beats category default`() {
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
        `when`(transactionRepository.deleteTransaction(transactionId, accountId, user))
            .thenThrow(LocalizedException.NotFound("error.transaction.notFound"))

        assertThrows(LocalizedException::class.java) {
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

        stubCategoryType(newCategoryId, CategoryType.EXPENSE)
        `when`(transactionRepository.fetchTransactionsByIds(accountId, ids, user)).thenReturn(oldTransactions)
        `when`(
            transactionRepository.updateCategoryForTransactions(accountId, ids, newCategoryId, user)
        ).thenReturn(2)

        val updated = service.bulkUpdateCategory(accountId, ids, newCategoryId, user)

        assertEquals(2, updated)
        verify(accountRepository, never()).updateBalance(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `bulkUpdateCategory rejects a managed target category`() {
        val ids = listOf(UUID.randomUUID())
        val managedCategoryId = 9L
        stubCategoryType(managedCategoryId, CategoryType.EXPENSE, isManaged = true)

        assertThrows(LocalizedException::class.java) {
            service.bulkUpdateCategory(accountId, ids, managedCategoryId, user)
        }
        verify(transactionRepository, never())
            .updateCategoryForTransactions(anyArg(), anyArg(), org.mockito.ArgumentMatchers.anyLong(), anyArg())
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

        assertThrows(LocalizedException::class.java) {
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
    fun `updateTransfer mutates both legs in place and re-applies balance deltas`() {
        val groupId = UUID.randomUUID()
        val sourceLegId = UUID.randomUUID()
        val destLegId = UUID.randomUUID()
        val destAccountId = UUID.randomUUID()
        val destAccount = BudgetAccountDTO(destAccountId, "Savings", BigDecimal("0.00"), BigDecimal("0.00"), Currency.EUR)
        lenient().`when`(accountRepository.fetchAccountById(destAccountId, user)).thenReturn(destAccount)

        val existing = TransactionDTO(id = sourceLegId, type = CategoryType.EXPENSE, amount = BigDecimal("100.00"), transferGroupId = groupId)
        `when`(transactionRepository.fetchTransactionById(sourceLegId, user)).thenReturn(existing)
        `when`(transactionRepository.fetchTransferLegs(groupId, user)).thenReturn(
            listOf(
                TransferLeg(sourceLegId, accountId, CategoryType.EXPENSE, BigDecimal("100.00")),
                TransferLeg(destLegId, destAccountId, CategoryType.INCOME, BigDecimal("100.00")),
            )
        )
        val newLegs = listOf(
            TransactionDTO(id = sourceLegId, type = CategoryType.EXPENSE, amount = BigDecimal("80.00"), transferGroupId = groupId),
            TransactionDTO(id = destLegId, type = CategoryType.INCOME, amount = BigDecimal("80.00"), transferGroupId = groupId),
        )
        `when`(
            transactionRepository.updateTransfer(
                eqArg(sourceLegId), eqArg(destLegId), eqArg(accountId), eqArg(destAccountId), anyArg(), anyArg(), eqArg(user)
            )
        ).thenReturn(newLegs)

        val form = TransferForm(BigDecimal("80.00"), destAccountId, "Adjusted", LocalDate.now())
        val result = service.updateTransfer(sourceLegId, accountId, form, user)

        assertEquals(2, result.size)
        assertEquals(sourceLegId, result[0].id)
        assertEquals(destLegId, result[1].id)
        verify(accountRepository).updateBalance(accountId, BigDecimal("100.00"), user)
        verify(accountRepository).updateBalance(destAccountId, BigDecimal("-100.00"), user)
        verify(accountRepository).updateBalance(accountId, BigDecimal("-80.00"), user)
        verify(accountRepository).updateBalance(destAccountId, BigDecimal("80.00"), user)
        verify(transactionRepository, never()).deleteTransactionsByIds(anyArg(), eqArg(user))
        verify(transactionRepository, never()).insertTransfer(anyArg(), anyArg(), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `updateTransfer rejects identical source and destination`() {
        val form = TransferForm(BigDecimal("10.00"), accountId, "Self", LocalDate.now())

        assertThrows(LocalizedException::class.java) {
            service.updateTransfer(UUID.randomUUID(), accountId, form, user)
        }

        verify(accountRepository, never()).updateBalance(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `updateTransfer rejects a non-transfer transaction`() {
        val txId = UUID.randomUUID()
        val destAccountId = UUID.randomUUID()
        `when`(transactionRepository.fetchTransactionById(txId, user)).thenReturn(
            TransactionDTO(id = txId, type = CategoryType.EXPENSE, amount = BigDecimal("10.00"), transferGroupId = null)
        )
        val form = TransferForm(BigDecimal("10.00"), destAccountId, "Not a transfer", LocalDate.now())

        assertThrows(LocalizedException::class.java) {
            service.updateTransfer(txId, accountId, form, user)
        }
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

        assertThrows(LocalizedException::class.java) {
            service.createBalanceAdjustment(accountId, form, user)
        }
    }

    @Test
    fun `createTransaction persists splits, updates balance, and alerts on split categories`() {
        val household = CategoryDTO(3L, "Household", "icon", "#0000FF", CategoryType.EXPENSE)
        val splits = listOf(
            TransactionSplitForm(categoryId = 1L, amount = BigDecimal("60.00")),
            TransactionSplitForm(categoryId = 3L, amount = BigDecimal("20.00")),
        )
        val form = TransactionForm(
            amount = BigDecimal("80.00"), categoryId = 1L, description = "Groceries run",
            transactionDate = LocalDate.now(), type = CategoryType.EXPENSE, splits = splits,
        )
        val txId = UUID.randomUUID()
        val created = TransactionDTO(
            id = txId, category = expenseCategory, type = CategoryType.EXPENSE,
            amount = BigDecimal("80.00"), description = "Groceries run", transactionDate = LocalDate.now(),
        )
        stubCategoryType(1L, CategoryType.EXPENSE)
        `when`(categoriesRepository.fetchCategoryClassifications(eqArg(user), eqArg(setOf(1L, 3L)))).thenReturn(
            mapOf(
                1L to CategoryClassification(CategoryType.EXPENSE, false),
                3L to CategoryClassification(CategoryType.EXPENSE, false),
            )
        )
        `when`(transactionRepository.createTransaction(eqArg(accountId), anyArg(), anyArg(), eqArg(user)))
            .thenReturn(created)
        `when`(transactionRepository.fetchSplitsByTransactionIds(user, listOf(txId))).thenReturn(
            mapOf(
                txId to listOf(
                    TransactionSplitDTO(UUID.randomUUID(), expenseCategory, BigDecimal("60.00")),
                    TransactionSplitDTO(UUID.randomUUID(), household, BigDecimal("20.00")),
                )
            )
        )

        val result = service.createTransaction(accountId, form, user)

        verify(transactionRepository).replaceSplits(eqArg(txId), eqArg(splits), eqArg(user))
        verify(accountRepository).updateBalance(accountId, BigDecimal("80.00").negate(), user)
        verify(notificationService).maybeRaiseBudgetAlerts(user, listOf(1L, 3L))
        assertEquals(2, result.splits.size)
    }

    @Test
    fun `createTransaction with a single split is rejected`() {
        val form = TransactionForm(
            amount = BigDecimal("80.00"), categoryId = 1L, description = "x",
            transactionDate = LocalDate.now(), type = CategoryType.EXPENSE,
            splits = listOf(TransactionSplitForm(1L, BigDecimal("80.00"))),
        )
        stubCategoryType(1L, CategoryType.EXPENSE)

        assertThrows(LocalizedException::class.java) { service.createTransaction(accountId, form, user) }
        verify(transactionRepository, never()).createTransaction(anyArg(), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `createTransaction split amounts not summing to the total is rejected`() {
        val form = TransactionForm(
            amount = BigDecimal("80.00"), categoryId = 1L, description = "x",
            transactionDate = LocalDate.now(), type = CategoryType.EXPENSE,
            splits = listOf(
                TransactionSplitForm(1L, BigDecimal("60.00")),
                TransactionSplitForm(3L, BigDecimal("10.00")),
            ),
        )
        stubCategoryType(1L, CategoryType.EXPENSE)

        assertThrows(LocalizedException::class.java) { service.createTransaction(accountId, form, user) }
        verify(transactionRepository, never()).createTransaction(anyArg(), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `createTransaction split into a managed category is rejected`() {
        val form = TransactionForm(
            amount = BigDecimal("80.00"), categoryId = 1L, description = "x",
            transactionDate = LocalDate.now(), type = CategoryType.EXPENSE,
            splits = listOf(
                TransactionSplitForm(1L, BigDecimal("60.00")),
                TransactionSplitForm(3L, BigDecimal("20.00")),
            ),
        )
        stubCategoryType(1L, CategoryType.EXPENSE)
        `when`(categoriesRepository.fetchCategoryClassifications(eqArg(user), eqArg(setOf(1L, 3L)))).thenReturn(
            mapOf(
                1L to CategoryClassification(CategoryType.EXPENSE, false),
                3L to CategoryClassification(CategoryType.EXPENSE, true),
            )
        )

        assertThrows(LocalizedException::class.java) { service.createTransaction(accountId, form, user) }
        verify(transactionRepository, never()).createTransaction(anyArg(), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `createTransaction split into a wrong-type category is rejected`() {
        val form = TransactionForm(
            amount = BigDecimal("80.00"), categoryId = 1L, description = "x",
            transactionDate = LocalDate.now(), type = CategoryType.EXPENSE,
            splits = listOf(
                TransactionSplitForm(1L, BigDecimal("60.00")),
                TransactionSplitForm(3L, BigDecimal("20.00")),
            ),
        )
        stubCategoryType(1L, CategoryType.EXPENSE)
        `when`(categoriesRepository.fetchCategoryClassifications(eqArg(user), eqArg(setOf(1L, 3L)))).thenReturn(
            mapOf(
                1L to CategoryClassification(CategoryType.EXPENSE, false),
                3L to CategoryClassification(CategoryType.INCOME, false),
            )
        )

        assertThrows(LocalizedException::class.java) { service.createTransaction(accountId, form, user) }
        verify(transactionRepository, never()).createTransaction(anyArg(), anyArg(), anyArg(), anyArg())
    }
}
