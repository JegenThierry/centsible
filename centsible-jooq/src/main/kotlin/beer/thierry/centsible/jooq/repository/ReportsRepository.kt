package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.reports.CashFlowPointDTO
import beer.thierry.centsible.api.model.reports.CategorySpendingSeriesDTO
import beer.thierry.centsible.api.model.reports.MonthlyCategoryAmountDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.ACCOUNT_HISTORY
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

private val TXN_MONTH_KEY = DSL.field(
    "to_char({0}, 'YYYY-MM')",
    String::class.java,
    TRANSACTIONS.TRANSACTION_DATE,
)

@Repository
class ReportsRepository(private val dsl: DSLContext) : IReportsRepository {

    override fun fetchAllUserSnapshotsUntil(
        until: OffsetDateTime,
        authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO> =
        dsl.select(
            ACCOUNT_HISTORY.ACCOUNT_ID,
            ACCOUNT_HISTORY.BALANCE,
            ACCOUNT_HISTORY.CREATED_AT,
        )
            .from(ACCOUNT_HISTORY)
            .where(
                ACCOUNT_HISTORY.USER_ID.eq(authenticatedUser.id)
                    .and(ACCOUNT_HISTORY.CREATED_AT.le(until))
            )
            .orderBy(ACCOUNT_HISTORY.CREATED_AT.asc())
            .fetchInto(BudgetAccountSnapshotDTO::class.java)

    override fun fetchCategorySpendingOverTime(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<CategorySpendingSeriesDTO> {
        val total = DSL.sum(TRANSACTIONS.AMOUNT)

        val rows = dsl.select(
            CATEGORIES.ID,
            CATEGORIES.NAME,
            CATEGORIES.COLOR,
            TXN_MONTH_KEY,
            total,
        )
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .where(
                ACCOUNTS.USER_ID.eq(authenticatedUser.id)
                    .and(TRANSACTIONS.TYPE.eq(CategoryType.EXPENSE.value))
                    .and(TRANSACTIONS.TRANSACTION_DATE.between(startDate, endDate))
                    .and(TRANSACTIONS.TRANSFER_GROUP_ID.isNull)
            )
            .groupBy(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.COLOR, TXN_MONTH_KEY)
            .fetch()

        return rows
            .groupBy { it[CATEGORIES.ID]!! }
            .map { (categoryId, group) ->
                val first = group.first()
                CategorySpendingSeriesDTO(
                    categoryId = categoryId,
                    categoryName = first[CATEGORIES.NAME] ?: "",
                    categoryColor = first[CATEGORIES.COLOR],
                    totals = group
                        .map { MonthlyCategoryAmountDTO(it[TXN_MONTH_KEY]!!, it[total] ?: BigDecimal.ZERO) }
                        .sortedBy { it.yearMonth },
                )
            }
            .sortedBy { it.categoryName }
    }

    override fun fetchCashFlow(
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO,
    ): List<CashFlowPointDTO> {
        val total = DSL.sum(TRANSACTIONS.AMOUNT)

        val rows = dsl.select(TXN_MONTH_KEY, TRANSACTIONS.TYPE, total)
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                ACCOUNTS.USER_ID.eq(authenticatedUser.id)
                    .and(TRANSACTIONS.TRANSACTION_DATE.between(startDate, endDate))
                    .and(TRANSACTIONS.TRANSFER_GROUP_ID.isNull)
            )
            .groupBy(TXN_MONTH_KEY, TRANSACTIONS.TYPE)
            .fetch()

        val income = mutableMapOf<String, BigDecimal>()
        val expense = mutableMapOf<String, BigDecimal>()
        for (r in rows) {
            val key = r[TXN_MONTH_KEY] ?: continue
            val amount = r[total] ?: BigDecimal.ZERO
            when (r[TRANSACTIONS.TYPE]) {
                CategoryType.INCOME.value -> income[key] = (income[key] ?: BigDecimal.ZERO) + amount
                CategoryType.EXPENSE.value -> expense[key] = (expense[key] ?: BigDecimal.ZERO) + amount
            }
        }

        val keys = (income.keys + expense.keys).distinct().sorted()
        return keys.map { k ->
            val i = income[k] ?: BigDecimal.ZERO
            val e = expense[k] ?: BigDecimal.ZERO
            CashFlowPointDTO(k, i, e, i - e)
        }
    }
}
