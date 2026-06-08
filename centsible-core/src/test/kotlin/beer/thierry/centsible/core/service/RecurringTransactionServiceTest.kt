package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.recurring.RecurringTransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.CategoryClassification
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.core.services.recurring.RecurringTransactionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class RecurringTransactionServiceTest {

    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

    @Mock
    private lateinit var repository: IRecurringTransactionRepository

    @Mock
    private lateinit var categoriesRepository: ICategoriesRepository

    @Mock
    private lateinit var accountRepository: IBudgetAccountsRepository

    @Mock
    private lateinit var currencyConversionService: ICurrencyConversionService

    @InjectMocks
    private lateinit var service: RecurringTransactionService

    private val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
    private val accountId = UUID.randomUUID()
    private val ownedCategoryId = 1L
    private val foreignCategoryId = 99L

    private fun form(categoryId: Long) = RecurringTransactionForm(
        amount = BigDecimal("10.00"),
        categoryId = categoryId,
        description = "Rent",
        frequency = Frequency.MONTHLY,
        startDate = LocalDate.now(),
    )

    private fun transferForm(destinationAccountId: UUID?) = RecurringTransactionForm(
        amount = BigDecimal("100.00"),
        description = "Monthly savings",
        frequency = Frequency.MONTHLY,
        startDate = LocalDate.now(),
        isTransfer = true,
        destinationAccountId = destinationAccountId,
    )

    private fun stubOwned(categoryId: Long) {
        `when`(categoriesRepository.fetchCategoryClassifications(user, listOf(categoryId)))
            .thenReturn(mapOf(categoryId to CategoryClassification(CategoryType.EXPENSE, false)))
    }

    private fun stubNotOwned(categoryId: Long) {
        `when`(categoriesRepository.fetchCategoryClassifications(user, listOf(categoryId)))
            .thenReturn(emptyMap())
    }

    @Test
    fun `create with an owned category persists the rule`() {
        val f = form(ownedCategoryId)
        val created = RecurringTransactionDTO(id = UUID.randomUUID())
        stubOwned(ownedCategoryId)
        `when`(repository.create(accountId, f, user)).thenReturn(created)

        val result = service.create(accountId, f, user)

        assertEquals(created, result)
        verify(repository).create(accountId, f, user)
    }

    @Test
    fun `create with another user's category is rejected and never hits the repository`() {
        val f = form(foreignCategoryId)
        stubNotOwned(foreignCategoryId)

        assertThrows(LocalizedException::class.java) {
            service.create(accountId, f, user)
        }

        verify(repository, never()).create(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `update with another user's category is rejected and never hits the repository`() {
        val f = form(foreignCategoryId)
        stubNotOwned(foreignCategoryId)

        assertThrows(LocalizedException::class.java) {
            service.update(UUID.randomUUID(), f, user)
        }

        verify(repository, never()).update(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `runMaterializationPass converts each occurrence to the account currency at its own date`() {
        val today = LocalDate.now()
        val rule = RecurringTransactionDTO(
            id = UUID.randomUUID(),
            accountId = accountId,
            category = CategoryDTO(ownedCategoryId, "Rent", "icon", "#fff", CategoryType.EXPENSE),
            amount = BigDecimal("10.00"),
            description = "Rent",
            frequency = Frequency.MONTHLY,
            startDate = today,
            nextRunAt = today,
            active = true,
            originalCurrency = Currency.USD,
        )
        val conversion = ConversionResult(
            convertedAmount = BigDecimal("9.07"),
            originalAmount = BigDecimal("10.00"),
            originalCurrency = Currency.USD,
            accountCurrency = Currency.EUR,
            rate = BigDecimal("0.907"),
            rateDate = today,
            sameCurrency = false,
        )
        `when`(repository.fetchDueRules(today)).thenReturn(listOf(rule))
        `when`(accountRepository.fetchAccountCurrency(accountId)).thenReturn(Currency.EUR)
        `when`(currencyConversionService.convert(BigDecimal("10.00"), Currency.USD, Currency.EUR, today))
            .thenReturn(conversion)
        `when`(repository.materializeOnce(rule, conversion)).thenReturn(null)

        val count = service.runMaterializationPass()

        assertEquals(1, count)
        verify(currencyConversionService).convert(BigDecimal("10.00"), Currency.USD, Currency.EUR, today)
        verify(repository).materializeOnce(rule, conversion)
    }

    @Test
    fun `create transfer rule verifies destination ownership and skips category check`() {
        val destAccountId = UUID.randomUUID()
        val f = transferForm(destAccountId)
        val created = RecurringTransactionDTO(id = UUID.randomUUID(), isTransfer = true)
        `when`(accountRepository.fetchAccountById(destAccountId, user))
            .thenReturn(BudgetAccountDTO(destAccountId, "Savings", BigDecimal.ZERO, BigDecimal.ZERO, Currency.EUR))
        `when`(repository.create(accountId, f, user)).thenReturn(created)

        val result = service.create(accountId, f, user)

        assertEquals(created, result)
        verify(accountRepository).fetchAccountById(destAccountId, user)
        verify(repository).create(accountId, f, user)
        verify(categoriesRepository, never()).fetchCategoryClassifications(anyArg(), anyArg())
    }

    @Test
    fun `create infers transfer when destination is provided even if isTransfer is false`() {
        val destAccountId = UUID.randomUUID()
        val f = transferForm(destAccountId).copy(isTransfer = false)
        val created = RecurringTransactionDTO(id = UUID.randomUUID(), isTransfer = true)
        `when`(accountRepository.fetchAccountById(destAccountId, user))
            .thenReturn(BudgetAccountDTO(destAccountId, "Savings", BigDecimal.ZERO, BigDecimal.ZERO, Currency.EUR))
        `when`(repository.create(accountId, f, user)).thenReturn(created)

        val result = service.create(accountId, f, user)

        assertEquals(created, result)
        verify(accountRepository).fetchAccountById(destAccountId, user)
        verify(repository).create(accountId, f, user)
        verify(categoriesRepository, never()).fetchCategoryClassifications(anyArg(), anyArg())
    }

    @Test
    fun `create transfer rule with destination equal to source is rejected`() {
        val f = transferForm(accountId)

        assertThrows(IllegalArgumentException::class.java) {
            service.create(accountId, f, user)
        }

        verify(repository, never()).create(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `create transfer rule without a destination is rejected`() {
        val f = transferForm(null)

        assertThrows(IllegalArgumentException::class.java) {
            service.create(accountId, f, user)
        }

        verify(repository, never()).create(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `runMaterializationPass for a transfer converts source currency to destination currency`() {
        val today = LocalDate.now()
        val destAccountId = UUID.randomUUID()
        val rule = RecurringTransactionDTO(
            id = UUID.randomUUID(),
            accountId = accountId,
            amount = BigDecimal("100.00"),
            description = "Savings",
            frequency = Frequency.MONTHLY,
            startDate = today,
            nextRunAt = today,
            active = true,
            isTransfer = true,
            destinationAccountId = destAccountId,
        )
        val conversion = ConversionResult(
            BigDecimal("110.00"), BigDecimal("100.00"), Currency.EUR, Currency.USD, BigDecimal("1.10"), today, false
        )
        `when`(repository.fetchDueRules(today)).thenReturn(listOf(rule))
        `when`(accountRepository.fetchAccountCurrency(accountId)).thenReturn(Currency.EUR)
        `when`(accountRepository.fetchAccountCurrency(destAccountId)).thenReturn(Currency.USD)
        `when`(currencyConversionService.convert(BigDecimal("100.00"), Currency.EUR, Currency.USD, today))
            .thenReturn(conversion)
        `when`(repository.materializeOnce(rule, conversion)).thenReturn(null)

        val count = service.runMaterializationPass()

        assertEquals(1, count)
        verify(currencyConversionService).convert(BigDecimal("100.00"), Currency.EUR, Currency.USD, today)
        verify(repository).materializeOnce(rule, conversion)
    }

    @Test
    fun `runMaterializationPass infers transfer when destination account exists and isTransfer is false`() {
        val today = LocalDate.now()
        val destAccountId = UUID.randomUUID()
        val rule = RecurringTransactionDTO(
            id = UUID.randomUUID(),
            accountId = accountId,
            amount = BigDecimal("100.00"),
            description = "Savings",
            frequency = Frequency.MONTHLY,
            startDate = today,
            nextRunAt = today,
            active = true,
            isTransfer = false,
            destinationAccountId = destAccountId,
        )
        val normalizedRule = rule.copy(isTransfer = true)
        val conversion = ConversionResult(
            BigDecimal("110.00"), BigDecimal("100.00"), Currency.EUR, Currency.USD, BigDecimal("1.10"), today, false
        )
        `when`(repository.fetchDueRules(today)).thenReturn(listOf(rule))
        `when`(accountRepository.fetchAccountCurrency(accountId)).thenReturn(Currency.EUR)
        `when`(accountRepository.fetchAccountCurrency(destAccountId)).thenReturn(Currency.USD)
        `when`(currencyConversionService.convert(BigDecimal("100.00"), Currency.EUR, Currency.USD, today))
            .thenReturn(conversion)
        `when`(repository.materializeOnce(normalizedRule, conversion)).thenReturn(null)

        val count = service.runMaterializationPass()

        assertEquals(1, count)
        verify(currencyConversionService).convert(BigDecimal("100.00"), Currency.EUR, Currency.USD, today)
        verify(repository).materializeOnce(normalizedRule, conversion)
    }
}
