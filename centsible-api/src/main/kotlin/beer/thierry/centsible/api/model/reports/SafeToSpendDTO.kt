package beer.thierry.centsible.api.model.reports

import java.math.BigDecimal

/**
 * Deterministic "safe to spend" for the current calendar month, computed user-wide (across all
 * accounts) in the user's default currency. No AI, no double-counting: actuals come from booked,
 * transfer-excluded transactions (ADR-0015) and upcoming amounts from recurring rules that have not
 * yet been materialized for the remainder of the month.
 *
 *   safeToSpend = (actualIncome + upcomingIncome) - alreadySpent - upcomingExpenses
 *
 * Every component is returned so the UI can show an auditable breakdown.
 */
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
