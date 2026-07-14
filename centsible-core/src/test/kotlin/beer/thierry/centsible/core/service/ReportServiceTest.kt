package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.core.services.reports.ReportService
import org.junit.jupiter.api.Assertions.assertEquals
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
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ReportServiceTest {

    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

    @Mock private lateinit var reportsRepository: IReportsRepository
    @Mock private lateinit var accountsRepository: IBudgetAccountsRepository
    @Mock private lateinit var budgetRepository: IBudgetRepository
    @Mock private lateinit var currencyConversionService: ICurrencyConversionService
    @Mock private lateinit var userRepository: IUserRepository
    @Mock private lateinit var recurringRepository: IRecurringTransactionRepository

    @InjectMocks
    private lateinit var service: ReportService

    private val user = UserDTO(defaultCurrency = "EUR")
    private val eurAccount = BudgetAccountDTO(id = UUID.randomUUID(), name = "eur", currency = Currency.EUR)
    private val usdAccount = BudgetAccountDTO(id = UUID.randomUUID(), name = "usd", currency = Currency.USD)

    private val startDate: LocalDate = LocalDate.now().minusDays(10)
    private val endDate: LocalDate = LocalDate.now()

    private fun snapshot(account: BudgetAccountDTO, balance: String, daysAgo: Long) = BudgetAccountSnapshotDTO(
        accountId = account.id,
        balance = BigDecimal(balance),
        createdAt = OffsetDateTime.of(LocalDate.now().minusDays(daysAgo), LocalTime.NOON, ZoneOffset.UTC),
    )

    private fun usdToEurRate(rate: String): ConversionResult = ConversionResult(
        convertedAmount = BigDecimal(rate),
        originalAmount = BigDecimal.ONE,
        originalCurrency = Currency.USD,
        accountCurrency = Currency.EUR,
        rate = BigDecimal(rate),
        rateDate = LocalDate.now(),
        sameCurrency = false,
    )

    @Test
    fun `fetchNetWorthOverTime converts foreign-currency balances into the user default currency`() {
        `when`(accountsRepository.fetchAllAccounts(user)).thenReturn(listOf(eurAccount, usdAccount))
        `when`(reportsRepository.fetchAllUserSnapshotsUntil(anyArg(), anyArg()))
            .thenReturn(listOf(snapshot(eurAccount, "100.00", 5), snapshot(usdAccount, "200.00", 4)))
        `when`(currencyConversionService.convert(BigDecimal.ONE, Currency.USD, Currency.EUR, LocalDate.now()))
            .thenReturn(usdToEurRate("0.90"))

        val points = service.fetchNetWorthOverTime(startDate, endDate, user)

        // 100 EUR + 200 USD * 0.90 = 280.00 EUR
        assertEquals(BigDecimal("280.00"), points.last().balance)
    }

    @Test
    fun `fetchNetWorthOverTime excludes accounts whose FX rate cannot be resolved`() {
        `when`(accountsRepository.fetchAllAccounts(user)).thenReturn(listOf(eurAccount, usdAccount))
        `when`(reportsRepository.fetchAllUserSnapshotsUntil(anyArg(), anyArg()))
            .thenReturn(listOf(snapshot(eurAccount, "100.00", 5), snapshot(usdAccount, "200.00", 4)))
        `when`(currencyConversionService.convert(BigDecimal.ONE, Currency.USD, Currency.EUR, LocalDate.now()))
            .thenThrow(RuntimeException("fx unavailable"))

        val points = service.fetchNetWorthOverTime(startDate, endDate, user)

        assertEquals(BigDecimal("100.00"), points.last().balance)
    }

    @Test
    fun `fetchNetWorthOverTime converts into the stored default currency, not the JWT principal's`() {
        // The principal always carries the fallback "EUR"; the user's real setting lives in the DB.
        `when`(userRepository.findUserById(user.id)).thenReturn(User(id = user.id, defaultCurrency = "USD"))
        `when`(accountsRepository.fetchAllAccounts(user)).thenReturn(listOf(eurAccount))
        `when`(reportsRepository.fetchAllUserSnapshotsUntil(anyArg(), anyArg()))
            .thenReturn(listOf(snapshot(eurAccount, "100.00", 5)))
        `when`(currencyConversionService.convert(BigDecimal.ONE, Currency.EUR, Currency.USD, LocalDate.now()))
            .thenReturn(
                ConversionResult(
                    convertedAmount = BigDecimal("1.10"),
                    originalAmount = BigDecimal.ONE,
                    originalCurrency = Currency.EUR,
                    accountCurrency = Currency.USD,
                    rate = BigDecimal("1.10"),
                    rateDate = LocalDate.now(),
                    sameCurrency = false,
                )
            )

        val points = service.fetchNetWorthOverTime(startDate, endDate, user)

        // 100 EUR * 1.10 = 110.00 USD
        assertEquals(BigDecimal("110.00"), points.last().balance)
    }

    @Test
    fun `fetchNetWorthOverTime never converts when all accounts use the default currency`() {
        `when`(accountsRepository.fetchAllAccounts(user)).thenReturn(listOf(eurAccount))
        `when`(reportsRepository.fetchAllUserSnapshotsUntil(anyArg(), anyArg()))
            .thenReturn(listOf(snapshot(eurAccount, "150.50", 3)))

        val points = service.fetchNetWorthOverTime(startDate, endDate, user)

        assertEquals(BigDecimal("150.50"), points.last().balance)
        verify(currencyConversionService, never()).convert(anyArg(), anyArg(), anyArg(), anyArg())
    }
}
