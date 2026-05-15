package beer.thierry.budgetplanner.core.service

import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.Currency
import beer.thierry.budgetplanner.api.model.category.CategoryDTO
import beer.thierry.budgetplanner.api.model.category.CategoryType
import beer.thierry.budgetplanner.api.model.transaction.TransactionDTO
import beer.thierry.budgetplanner.api.model.transaction.TransactionForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.IBudgetAccountHistoryRepository
import beer.thierry.budgetplanner.api.repository.IBudgetAccountsRepository
import beer.thierry.budgetplanner.api.repository.ITransactionRepository
import beer.thierry.budgetplanner.core.services.transactions.TransactionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class TransactionServiceTest {

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()
    private fun <T> eq(value: T): T = org.mockito.ArgumentMatchers.eq(value) ?: value

    @Mock
    private lateinit var transactionRepository: ITransactionRepository

    @Mock
    private lateinit var accountRepository: IBudgetAccountsRepository

    @Mock
    private lateinit var accountHistoryRepository: IBudgetAccountHistoryRepository

    @InjectMocks
    private lateinit var service: TransactionService

    private val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
    private val accountId = UUID.randomUUID()
    private val category = CategoryDTO(1L, "Category", "icon", "#FF0000", CategoryType.EXPENSE)
    private val incomeCategory = CategoryDTO(2L, "Income Category", "icon", "#00FF00", CategoryType.INCOME)

    @Test
    fun `createTransaction should update balance for INCOME`() {
        val form = TransactionForm(BigDecimal("50.00"), 2L, "Income", LocalDate.now())
        val transaction = TransactionDTO(
            UUID.randomUUID(),
            incomeCategory,
            BigDecimal("50.00"),
            "Income",
            LocalDate.now(),
            null,
            null
        )
        val account =
            BudgetAccountDTO(accountId, "Account", BigDecimal("150.00"), BigDecimal("100.00"), Currency.EUR)

        `when`(transactionRepository.createTransaction(accountId, form, user)).thenReturn(transaction)

        val result = service.createTransaction(accountId, form, user)

        assertEquals(transaction, result)
        verify(accountRepository).updateBalance(accountId, BigDecimal("50.00"), user)
    }

    @Test
    fun `createTransaction should update balance for EXPENSE`() {
        val form = TransactionForm(BigDecimal("30.00"), 1L, "Expense", LocalDate.now())
        val transaction = TransactionDTO(
            UUID.randomUUID(),
            category,
            BigDecimal("30.00"),
            "Expense",
            LocalDate.now(),
            null,
            null
        )
        val account =
            BudgetAccountDTO(accountId, "Account", BigDecimal("70.00"), BigDecimal("100.00"), Currency.EUR)

        `when`(transactionRepository.createTransaction(accountId, form, user)).thenReturn(transaction)

        val result = service.createTransaction(accountId, form, user)

        assertEquals(transaction, result)
        verify(accountRepository).updateBalance(accountId, BigDecimal("-30.00"), user)
    }

    @Test
    fun `updateTransaction should reverse old and apply new transaction`() {
        val transactionId = UUID.randomUUID()
        val form = TransactionForm(BigDecimal("100.00"), 2L, "New Income", LocalDate.now())

        val oldTransaction = TransactionDTO(
            transactionId,
            category,
            BigDecimal("50.00"),
            "Old Expense",
            LocalDate.now(),
            null,
            null
        )
        val updatedTransaction = TransactionDTO(
            transactionId,
            incomeCategory,
            BigDecimal("100.00"),
            "New Income",
            LocalDate.now(),
            null,
            null
        )
        val account =
            BudgetAccountDTO(accountId, "Account", BigDecimal("250.00"), BigDecimal("100.00"), Currency.EUR)

        `when`(transactionRepository.fetchTransactionById(transactionId, user)).thenReturn(oldTransaction)
        `when`(transactionRepository.updateTransaction(transactionId, accountId, form, user)).thenReturn(
            updatedTransaction
        )

        val result = service.updateTransaction(transactionId, accountId, form, user)

        assertEquals(updatedTransaction, result)
        // Combined adjustment: reverse old expense (+50) + apply new income (+100) = +150
        verify(accountRepository).updateBalance(accountId, BigDecimal("150.00"), user)
    }

    @Test
    fun `deleteTransaction should reverse transaction`() {
        val transactionId = UUID.randomUUID()
        val transaction = TransactionDTO(
            transactionId,
            incomeCategory,
            BigDecimal("40.00"),
            "Income",
            LocalDate.now(),
            null,
            null
        )
        val account =
            BudgetAccountDTO(accountId, "Account", BigDecimal("60.00"), BigDecimal("100.00"), Currency.EUR)

        `when`(transactionRepository.deleteTransaction(transactionId, user)).thenReturn(transaction)

        val result = service.deleteTransaction(transactionId, accountId, user)

        assertEquals(transaction, result)
        // Reverse income: -40
        verify(accountRepository).updateBalance(accountId, BigDecimal("-40.00"), user)
    }

    @Test
    fun `deleteTransaction should reverse EXPENSE`() {
        val transactionId = UUID.randomUUID()
        val transaction = TransactionDTO(
            transactionId,
            category,
            BigDecimal("25.00"),
            "Expense",
            LocalDate.now(),
            null,
            null
        )
        val account =
            BudgetAccountDTO(accountId, "Account", BigDecimal("125.00"), BigDecimal("100.00"), Currency.EUR)

        `when`(transactionRepository.deleteTransaction(transactionId, user)).thenReturn(transaction)

        val result = service.deleteTransaction(transactionId, accountId, user)

        assertEquals(transaction, result)
        // Reverse expense: +25
        verify(accountRepository).updateBalance(accountId, BigDecimal("25.00"), user)
    }

    @Test
    fun `updateTransaction should reverse old INCOME and apply new EXPENSE`() {
        val transactionId = UUID.randomUUID()
        val form = TransactionForm(BigDecimal("80.00"), 1L, "New Expense", LocalDate.now())

        val oldTransaction = TransactionDTO(
            transactionId,
            incomeCategory,
            BigDecimal("120.00"),
            "Old Income",
            LocalDate.now(),
            null,
            null
        )
        val updatedTransaction = TransactionDTO(
            transactionId,
            category,
            BigDecimal("80.00"),
            "New Expense",
            LocalDate.now(),
            null,
            null
        )
        val account =
            BudgetAccountDTO(accountId, "Account", BigDecimal("0.00"), BigDecimal("100.00"), Currency.EUR)

        `when`(transactionRepository.fetchTransactionById(transactionId, user)).thenReturn(oldTransaction)
        `when`(transactionRepository.updateTransaction(transactionId, accountId, form, user)).thenReturn(
            updatedTransaction
        )

        val result = service.updateTransaction(transactionId, accountId, form, user)

        assertEquals(updatedTransaction, result)
        // Combined adjustment: reverse old income (-120) + apply new expense (-80) = -200
        verify(accountRepository).updateBalance(accountId, BigDecimal("-200.00"), user)
    }
}
