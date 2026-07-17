package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
import beer.thierry.centsible.api.model.notification.NotificationType
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.repository.ILoansRepository
import beer.thierry.centsible.api.repository.INotificationRepository
import beer.thierry.centsible.api.repository.IProviderConnectionsRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.core.services.notifications.NotificationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationServiceTest {

    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

    private fun <T> eqArg(value: T): T = org.mockito.ArgumentMatchers.eq(value)

    @Mock private lateinit var notifications: INotificationRepository
    @Mock private lateinit var budgets: IBudgetRepository
    @Mock private lateinit var users: IUserRepository
    @Mock private lateinit var loans: ILoansRepository
    @Mock private lateinit var recurring: IRecurringTransactionRepository
    @Mock private lateinit var accounts: IBudgetAccountsRepository
    @Mock private lateinit var connections: IProviderConnectionsRepository
    @Mock private lateinit var currencyConversionService: ICurrencyConversionService

    @InjectMocks
    private lateinit var service: NotificationService

    private val user = UserDTO(id = UUID.randomUUID())
    private val account = BudgetAccountDTO(
        id = UUID.randomUUID(),
        name = "Checking",
        balance = BigDecimal("150.00"),
        currency = Currency.EUR,
    )
    private val jpyAccount = BudgetAccountDTO(
        id = UUID.randomUUID(),
        name = "Yen savings",
        balance = BigDecimal("150.00"),
        currency = Currency.JPY,
    )

    private fun settings(threshold: String?) =
        NotificationSettingsDTO(lowBalanceThreshold = threshold?.let { BigDecimal(it) })

    private fun expenseRule(amount: String, daysAhead: Long) = RecurringTransactionDTO(
        id = UUID.randomUUID(),
        accountId = account.id,
        amount = BigDecimal(amount),
        frequency = Frequency.MONTHLY,
        nextRunAt = LocalDate.now().plusDays(daysAhead),
        active = true,
        type = CategoryType.EXPENSE,
    )

    private fun openLoan(dueInDays: Long, outstanding: String = "120.00") = LoanDTO(
        id = UUID.randomUUID(),
        contact = ContactDTO(id = UUID.randomUUID(), name = "Sam"),
        outstanding = BigDecimal(outstanding),
        currency = "EUR",
        dueDate = LocalDate.now().plusDays(dueInDays),
    )

    @Test
    fun `projects a shortfall when a recurring expense pushes the account below the threshold`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(settings("100.00"))
        `when`(accounts.fetchAllAccounts(user)).thenReturn(listOf(account))
        `when`(recurring.fetchAll(user, null)).thenReturn(listOf(expenseRule("80.00", 5)))

        service.runScheduledChecks(user)

        verify(notifications, times(1))
            .create(anyArg(), eqArg(NotificationType.PROJECTED_SHORTFALL), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `stays quiet when the projected balance never dips below the threshold`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(settings("100.00"))
        `when`(accounts.fetchAllAccounts(user)).thenReturn(listOf(account))
        `when`(recurring.fetchAll(user, null)).thenReturn(listOf(expenseRule("30.00", 5)))

        service.runScheduledChecks(user)

        verify(notifications, never())
            .create(anyArg(), eqArg(NotificationType.PROJECTED_SHORTFALL), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `a recurring transfer out of the account counts against its projection`() {
        val transferOut = RecurringTransactionDTO(
            id = UUID.randomUUID(),
            accountId = account.id,
            destinationAccountId = UUID.randomUUID(),
            amount = BigDecimal("90.00"),
            frequency = Frequency.MONTHLY,
            nextRunAt = LocalDate.now().plusDays(4),
            active = true,
            isTransfer = true,
        )
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(settings("100.00"))
        `when`(accounts.fetchAllAccounts(user)).thenReturn(listOf(account))
        `when`(recurring.fetchAll(user, null)).thenReturn(listOf(transferOut))

        service.runScheduledChecks(user)

        verify(notifications, times(1))
            .create(anyArg(), eqArg(NotificationType.PROJECTED_SHORTFALL), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `credits a cross-currency transfer destination with the converted amount`() {
        val transferIn = RecurringTransactionDTO(
            id = UUID.randomUUID(),
            accountId = account.id,
            destinationAccountId = jpyAccount.id,
            amount = BigDecimal("1.00"),
            frequency = Frequency.MONTHLY,
            nextRunAt = LocalDate.now().plusDays(4),
            active = true,
            isTransfer = true,
        )
        val jpyExpense = RecurringTransactionDTO(
            id = UUID.randomUUID(),
            accountId = jpyAccount.id,
            amount = BigDecimal("100.00"),
            frequency = Frequency.MONTHLY,
            nextRunAt = LocalDate.now().plusDays(5),
            active = true,
            type = CategoryType.EXPENSE,
        )
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(settings("100.00"))
        `when`(accounts.fetchAllAccounts(user)).thenReturn(listOf(account, jpyAccount))
        `when`(recurring.fetchAll(user, null)).thenReturn(listOf(transferIn, jpyExpense))
        `when`(currencyConversionService.convert(BigDecimal("1.00"), Currency.EUR, Currency.JPY, LocalDate.now()))
            .thenReturn(
                ConversionResult(
                    convertedAmount = BigDecimal("160.00"),
                    originalAmount = BigDecimal("1.00"),
                    originalCurrency = Currency.EUR,
                    accountCurrency = Currency.JPY,
                    rate = BigDecimal("160.00"),
                    rateDate = LocalDate.now(),
                    sameCurrency = false,
                )
            )

        service.runScheduledChecks(user)

        verify(notifications, never())
            .create(anyArg(), eqArg(NotificationType.PROJECTED_SHORTFALL), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `skips a cross-currency transfer credit when its FX rate cannot be resolved`() {
        val transferIn = RecurringTransactionDTO(
            id = UUID.randomUUID(),
            accountId = account.id,
            destinationAccountId = jpyAccount.id,
            amount = BigDecimal("1.00"),
            frequency = Frequency.MONTHLY,
            nextRunAt = LocalDate.now().plusDays(4),
            active = true,
            isTransfer = true,
        )
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(settings("100.00"))
        `when`(accounts.fetchAllAccounts(user)).thenReturn(listOf(account, jpyAccount))
        `when`(recurring.fetchAll(user, null)).thenReturn(listOf(transferIn))
        `when`(currencyConversionService.convert(BigDecimal("1.00"), Currency.EUR, Currency.JPY, LocalDate.now()))
            .thenThrow(RuntimeException("fx unavailable"))

        service.runScheduledChecks(user)

        verify(notifications, never())
            .create(anyArg(), eqArg(NotificationType.PROJECTED_SHORTFALL), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `does not project when no low-balance threshold is configured`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(settings(null))

        service.runScheduledChecks(user)

        verify(notifications, never())
            .create(anyArg(), eqArg(NotificationType.PROJECTED_SHORTFALL), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `warns about a loan approaching its due date`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(NotificationSettingsDTO())
        `when`(loans.fetchAllLoans(user)).thenReturn(listOf(openLoan(dueInDays = 2)))

        service.runScheduledChecks(user)

        verify(notifications, times(1))
            .create(anyArg(), eqArg(NotificationType.LOAN_DUE), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `warns about an overdue loan`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(NotificationSettingsDTO())
        `when`(loans.fetchAllLoans(user)).thenReturn(listOf(openLoan(dueInDays = -3)))

        service.runScheduledChecks(user)

        verify(notifications, times(1))
            .create(anyArg(), eqArg(NotificationType.LOAN_DUE), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `stays quiet for a loan due beyond the look-ahead window`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(NotificationSettingsDTO())
        `when`(loans.fetchAllLoans(user)).thenReturn(listOf(openLoan(dueInDays = 10)))

        service.runScheduledChecks(user)

        verify(notifications, never())
            .create(anyArg(), eqArg(NotificationType.LOAN_DUE), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `warns about a recurring rule running within the look-ahead window`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(NotificationSettingsDTO())
        `when`(recurring.fetchAll(user, null)).thenReturn(listOf(expenseRule("40.00", 1)))

        service.runScheduledChecks(user)

        verify(notifications, times(1))
            .create(anyArg(), eqArg(NotificationType.RECURRING_UPCOMING), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `stays quiet for a recurring rule running beyond the look-ahead window`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(NotificationSettingsDTO())
        `when`(recurring.fetchAll(user, null)).thenReturn(listOf(expenseRule("40.00", 10)))

        service.runScheduledChecks(user)

        verify(notifications, never())
            .create(anyArg(), eqArg(NotificationType.RECURRING_UPCOMING), anyArg(), anyArg(), anyArg())
    }

    private fun overBudget() = BudgetDTO(
        id = UUID.randomUUID(),
        category = CategoryDTO(id = 1L, name = "Groceries"),
        amountLimit = BigDecimal("100.00"),
        amountSpent = BigDecimal("120.00"),
        period = "2099-01",
    )

    @Test
    fun `raises a budget-exceeded alert when spending is over the limit`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(NotificationSettingsDTO())
        `when`(budgets.fetchAllWithSpentForMonth(user, YearMonth.now())).thenReturn(listOf(overBudget()))

        service.maybeRaiseBudgetAlerts(user, null)

        verify(notifications, times(1))
            .create(anyArg(), eqArg(NotificationType.BUDGET_EXCEEDED), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `does not raise budget alerts when the user has turned them off`() {
        `when`(users.fetchNotificationSettings(user.id)).thenReturn(NotificationSettingsDTO(budgetAlertsEnabled = false))

        service.maybeRaiseBudgetAlerts(user, null)

        verify(notifications, never()).create(anyArg(), anyArg(), anyArg(), anyArg(), anyArg())
    }
}
