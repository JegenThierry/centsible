package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.reports.CashFlowCurrencyPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingCurrencyPointDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.ACCOUNT_HISTORY
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import beer.thierry.jooq.generated.tables.references.TRANSACTION_SPLITS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@Repository
class ReportsRepository(private val dsl: DSLContext) : IReportsRepository {

    override fun fetchUserSnapshotsBetween(
        from: OffsetDateTime,
        until: OffsetDateTime,
        authenticatedUser: UserDTO,
    ): List<BudgetAccountSnapshotDTO> =
        dsl.select(
            ACCOUNT_HISTORY.ACCOUNT_ID,
            ACCOUNT_HISTORY.BALANCE,
            ACCOUNT_HISTORY.CREATED_AT,
        )
            .from(ACCOUNT_HISTORY)
            .where(
                ACCOUNT_HISTORY.USER_ID.eq(authenticatedUser.id)
                    .and(ACCOUNT_HISTORY.CREATED_AT.between(from, until))
            )
            .orderBy(ACCOUNT_HISTORY.CREATED_AT.asc())
            .fetchInto(BudgetAccountSnapshotDTO::class.java)

    override fun fetchLatestSnapshotPerAccountAsOf(
        until: OffsetDateTime,
        authenticatedUser: UserDTO,
    ): List<BudgetAccountSnapshotDTO> =
        dsl.selectDistinct(
            ACCOUNT_HISTORY.ACCOUNT_ID,
            ACCOUNT_HISTORY.BALANCE,
            ACCOUNT_HISTORY.CREATED_AT,
        )
            .on(ACCOUNT_HISTORY.ACCOUNT_ID)
            .from(ACCOUNT_HISTORY)
            .where(
                ACCOUNT_HISTORY.USER_ID.eq(authenticatedUser.id)
                    .and(ACCOUNT_HISTORY.CREATED_AT.le(until))
            )
            .orderBy(ACCOUNT_HISTORY.ACCOUNT_ID, ACCOUNT_HISTORY.CREATED_AT.desc())
            .fetchInto(BudgetAccountSnapshotDTO::class.java)

    override fun fetchCategorySpendingOverTime(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<CategorySpendingCurrencyPointDTO> {
        val effectiveAmount = effectiveAmount()
        val effectiveCategoryId = effectiveCategoryId()
        val total = DSL.sum(effectiveAmount)

        return dsl.select(
            CATEGORIES.ID,
            CATEGORIES.NAME,
            CATEGORIES.COLOR,
            ACCOUNTS.CURRENCY,
            TXN_MONTH_KEY,
            total,
        )
            .from(TRANSACTIONS)
            .leftJoin(TRANSACTION_SPLITS).on(TRANSACTION_SPLITS.TRANSACTION_ID.eq(TRANSACTIONS.ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .join(CATEGORIES).on(CATEGORIES.ID.eq(effectiveCategoryId))
            .where(
                ACCOUNTS.USER_ID.eq(authenticatedUser.id)
                    .and(expenseInPeriod(startDate, endDate))
            )
            .groupBy(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.COLOR, ACCOUNTS.CURRENCY, TXN_MONTH_KEY)
            .fetch()
            .map { r ->
                CategorySpendingCurrencyPointDTO(
                    categoryId = r[CATEGORIES.ID]!!,
                    categoryName = r[CATEGORIES.NAME] ?: "",
                    categoryColor = r[CATEGORIES.COLOR],
                    yearMonth = r[TXN_MONTH_KEY]!!,
                    currency = Currency.valueOf(r[ACCOUNTS.CURRENCY]!!),
                    amount = r[total] ?: BigDecimal.ZERO,
                )
            }
    }

    override fun fetchCashFlow(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<CashFlowCurrencyPointDTO> {
        val total = DSL.sum(TRANSACTIONS.AMOUNT)

        val rows = dsl.select(TXN_MONTH_KEY, ACCOUNTS.CURRENCY, TRANSACTIONS.TYPE, total)
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                ACCOUNTS.USER_ID.eq(authenticatedUser.id)
                    .and(TRANSACTIONS.TRANSACTION_DATE.between(startDate, endDate))
                    .and(TRANSACTIONS.TRANSFER_GROUP_ID.isNull)
            )
            .groupBy(TXN_MONTH_KEY, ACCOUNTS.CURRENCY, TRANSACTIONS.TYPE)
            .fetch()

        val income = mutableMapOf<Pair<String, Currency>, BigDecimal>()
        val expense = mutableMapOf<Pair<String, Currency>, BigDecimal>()
        for (r in rows) {
            val key = (r[TXN_MONTH_KEY] ?: continue) to Currency.valueOf(r[ACCOUNTS.CURRENCY]!!)
            val amount = r[total] ?: BigDecimal.ZERO
            when (r[TRANSACTIONS.TYPE]) {
                CategoryType.INCOME.value -> income[key] = (income[key] ?: BigDecimal.ZERO) + amount
                CategoryType.EXPENSE.value -> expense[key] = (expense[key] ?: BigDecimal.ZERO) + amount
            }
        }

        val keys = (income.keys + expense.keys).distinct().sortedWith(compareBy({ it.first }, { it.second }))
        return keys.map { (yearMonth, currency) ->
            CashFlowCurrencyPointDTO(
                yearMonth = yearMonth,
                currency = currency,
                income = income[yearMonth to currency] ?: BigDecimal.ZERO,
                expense = expense[yearMonth to currency] ?: BigDecimal.ZERO,
            )
        }
    }
}
