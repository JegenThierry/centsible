package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.reports.CashFlowPointDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.centsible.core.services.reports.SafeToSpendService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SafeToSpendServiceTest {

    // Mockito's any() returns null which Kotlin's non-null types reject; reify the type.
    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

    @Mock
    private lateinit var reportsRepository: IReportsRepository

    @Mock
    private lateinit var recurringRepository: IRecurringTransactionRepository

    @InjectMocks
    private lateinit var service: SafeToSpendService

    private val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)

    private fun money(v: String) = BigDecimal(v)

    private fun cashFlow(income: String, expense: String): CashFlowPointDTO {
        val i = money(income)
        val e = money(expense)
        return CashFlowPointDTO(YearMonth.now().toString(), i, e, i - e)
    }

    private fun rule(
        amount: BigDecimal?,
        type: CategoryType?,
        frequency: Frequency? = Frequency.MONTHLY,
        nextRunAt: LocalDate? = LocalDate.now(),
        endDate: LocalDate? = null,
        active: Boolean = true,
        isTransfer: Boolean = false,
    ) = RecurringTransactionDTO(
        amount = amount,
        type = type,
        frequency = frequency,
        nextRunAt = nextRunAt,
        endDate = endDate,
        active = active,
        isTransfer = isTransfer,
    )

    private fun stub(cashFlow: List<CashFlowPointDTO>, rules: List<RecurringTransactionDTO>) {
        `when`(reportsRepository.fetchCashFlow(anyArg(), anyArg(), anyArg())).thenReturn(cashFlow)
        `when`(recurringRepository.fetchAll(anyArg(), anyArg())).thenReturn(rules)
    }

    @Test
    fun `empty inputs yield a zero, well-formed result`() {
        stub(emptyList(), emptyList())

        val result = service.fetchForCurrentMonth(user)

        assertEquals(money("0.00"), result.actualIncome)
        assertEquals(money("0.00"), result.upcomingIncome)
        assertEquals(money("0.00"), result.expectedIncome)
        assertEquals(money("0.00"), result.alreadySpent)
        assertEquals(money("0.00"), result.upcomingExpenses)
        assertEquals(money("0.00"), result.safeToSpend)
        assertEquals(money("0.00"), result.dailyAllowance)
        assertEquals("EUR", result.currency)
        assertEquals(YearMonth.now().toString(), result.yearMonth)
        assertTrue(result.daysRemaining in 1..31)
    }

    @Test
    fun `computes safe-to-spend from booked actuals only`() {
        stub(listOf(cashFlow(income = "3000", expense = "1200")), emptyList())

        val result = service.fetchForCurrentMonth(user)

        assertEquals(money("3000.00"), result.expectedIncome)
        assertEquals(money("1200.00"), result.alreadySpent)
        assertEquals(money("0.00"), result.upcomingExpenses)
        assertEquals(money("1800.00"), result.safeToSpend)
        // dailyAllowance spreads the (positive) headroom over the days left in the month.
        val expectedDaily = money("1800.00").divide(result.daysRemaining.toBigDecimal(), 2, RoundingMode.HALF_UP)
        assertEquals(expectedDaily, result.dailyAllowance)
    }

    @Test
    fun `adds upcoming recurring income and expenses for the rest of the month`() {
        stub(
            listOf(cashFlow(income = "1000", expense = "300")),
            listOf(
                rule(amount = money("2000"), type = CategoryType.INCOME),   // monthly, due today -> 1x
                rule(amount = money("800"), type = CategoryType.EXPENSE),   // monthly, due today -> 1x
            ),
        )

        val result = service.fetchForCurrentMonth(user)

        assertEquals(money("2000.00"), result.upcomingIncome)
        assertEquals(money("3000.00"), result.expectedIncome)
        assertEquals(money("300.00"), result.alreadySpent)
        assertEquals(money("800.00"), result.upcomingExpenses)
        assertEquals(money("1900.00"), result.safeToSpend)
    }

    @Test
    fun `excludes inactive, transfer, far-future and incomplete recurring rules`() {
        stub(
            listOf(cashFlow(income = "1000", expense = "0")),
            listOf(
                rule(amount = money("500"), type = CategoryType.EXPENSE, active = false),
                rule(amount = money("500"), type = CategoryType.EXPENSE, isTransfer = true),
                rule(amount = money("500"), type = CategoryType.EXPENSE, nextRunAt = LocalDate.now().plusMonths(2)),
                rule(amount = null, type = CategoryType.EXPENSE),
                rule(amount = money("500"), type = null),
            ),
        )

        val result = service.fetchForCurrentMonth(user)

        assertEquals(money("0.00"), result.upcomingExpenses)
        assertEquals(money("1000.00"), result.expectedIncome)
        assertEquals(money("1000.00"), result.safeToSpend)
    }

    @Test
    fun `counts only occurrences inside the remaining month and respects endDate`() {
        // Daily rule that started 5 days ago but ends today: only today falls in [today, endOfMonth].
        stub(
            emptyList(),
            listOf(
                rule(
                    amount = money("10"),
                    type = CategoryType.EXPENSE,
                    frequency = Frequency.DAILY,
                    nextRunAt = LocalDate.now().minusDays(5),
                    endDate = LocalDate.now(),
                ),
            ),
        )

        val result = service.fetchForCurrentMonth(user)

        assertEquals(money("10.00"), result.upcomingExpenses)
        assertEquals(money("-10.00"), result.safeToSpend)
    }

    @Test
    fun `clamps daily allowance to zero when safe-to-spend is negative`() {
        stub(listOf(cashFlow(income = "100", expense = "500")), emptyList())

        val result = service.fetchForCurrentMonth(user)

        assertEquals(money("-400.00"), result.safeToSpend)
        assertEquals(money("0.00"), result.dailyAllowance)
    }
}
