package beer.thierry.centsible.api.model.reports

import java.math.BigDecimal

data class SafeToSpendDTO(
    val yearMonth: String,
    val currency: String,
    val actualIncome: BigDecimal,
    val upcomingIncome: BigDecimal,
    val expectedIncome: BigDecimal,
    val alreadySpent: BigDecimal,
    val upcomingExpenses: BigDecimal,
    val safeToSpend: BigDecimal,
    val daysRemaining: Int,
    val dailyAllowance: BigDecimal,
)
