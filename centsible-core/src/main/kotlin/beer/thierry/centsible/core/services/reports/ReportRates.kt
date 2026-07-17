package beer.thierry.centsible.core.services.reports

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.reports.CashFlowCurrencyPointDTO
import beer.thierry.centsible.api.model.reports.CashFlowPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingCurrencyPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingSeriesDTO
import beer.thierry.centsible.api.model.reports.MonthlyCategoryAmountDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

private val log = LoggerFactory.getLogger("beer.thierry.centsible.core.services.reports.ReportRates")

/**
 * Today's FX multiplier into [target], resolved once per user-facing report. Currencies whose rate
 * cannot be resolved map to null and are excluded from totals (mirrors LoanService.totalOutstanding).
 */
internal data class ConversionRates(
    val target: Currency,
    val byCurrency: Map<Currency, BigDecimal?>,
    val byAccount: Map<UUID, BigDecimal?>,
)

/**
 * Single source of the report-side FX rates, shared by [ReportService] and [SafeToSpendService] so
 * net worth, cash flow and safe-to-spend all convert against the same target and exclude the same
 * unresolvable currencies instead of each re-spelling the conversion.
 */
internal fun accountRatesFor(
    authenticatedUser: UserDTO,
    accounts: IBudgetAccountsRepository,
    users: IUserRepository,
    currencyConversion: ICurrencyConversionService,
): ConversionRates {
    // The JWT principal doesn't carry defaultCurrency (it would go stale anyway) — re-fetch,
    // mirroring LoanService.totalOutstanding.
    val target = Currency.parseOrNull(
        users.findUserById(authenticatedUser.id)?.defaultCurrency ?: authenticatedUser.defaultCurrency
    ) ?: Currency.EUR
    val today = LocalDate.now()
    val userAccounts = accounts.fetchAllAccounts(authenticatedUser)
    val rateByCurrency = userAccounts.map { it.currency }.distinct().associateWith { currency ->
        if (currency == target) {
            BigDecimal.ONE
        } else {
            runCatching { currencyConversion.convert(BigDecimal.ONE, currency, target, today).rate }
                .getOrElse {
                    log.warn("Excluding {} accounts from report totals: FX {}->{} unavailable", currency, currency, target, it)
                    null
                }
        }
    }
    return ConversionRates(target, rateByCurrency, userAccounts.associate { it.id to rateByCurrency[it.currency] })
}

/**
 * Folds per-currency buckets into one monthly series in [ConversionRates.target]. Each bucket is
 * converted before it is summed; buckets whose currency has no resolvable rate drop out, exactly as
 * their accounts drop out of net worth.
 */
@JvmName("cashFlowConvertedInto")
internal fun List<CashFlowCurrencyPointDTO>.convertedInto(rates: ConversionRates): List<CashFlowPointDTO> {
    val income = mutableMapOf<String, BigDecimal>()
    val expense = mutableMapOf<String, BigDecimal>()
    for (bucket in this) {
        val rate = rates.byCurrency[bucket.currency] ?: continue
        income.merge(bucket.yearMonth, bucket.income.multiply(rate), BigDecimal::add)
        expense.merge(bucket.yearMonth, bucket.expense.multiply(rate), BigDecimal::add)
    }
    return (income.keys + expense.keys).distinct().sorted().map { key ->
        val i = (income[key] ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
        val e = (expense[key] ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
        CashFlowPointDTO(key, i, e, i - e)
    }
}

/**
 * Folds per-currency rows into one series per category in [ConversionRates.target], on the same terms
 * as the cash-flow overload — convert first, then sum, and drop currencies with no resolvable rate —
 * so a year-over-year card's per-category rows agree with its totals.
 */
@JvmName("categorySpendingConvertedInto")
internal fun List<CategorySpendingCurrencyPointDTO>.convertedInto(
    rates: ConversionRates,
): List<CategorySpendingSeriesDTO> =
    mapNotNull { row -> rates.byCurrency[row.currency]?.let { row to it } }
        .groupBy { (row, _) -> row.categoryId }
        .map { (categoryId, group) ->
            val first = group.first().first
            val byMonth = sortedMapOf<String, BigDecimal>()
            for ((row, rate) in group) byMonth.merge(row.yearMonth, row.amount.multiply(rate), BigDecimal::add)
            CategorySpendingSeriesDTO(
                categoryId = categoryId,
                categoryName = first.categoryName,
                categoryColor = first.categoryColor,
                totals = byMonth.map { (yearMonth, amount) ->
                    MonthlyCategoryAmountDTO(yearMonth, amount.setScale(2, RoundingMode.HALF_UP))
                },
            )
        }
        .sortedBy { it.categoryName }
