package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import beer.thierry.jooq.generated.tables.references.TRANSACTION_SPLITS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record1
import org.jooq.Select
import org.jooq.impl.DSL
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal object ManagedCategoryNames {
    const val LENDING = "Lending"
    const val REPAYMENT = "Repayment"
    const val TRANSFER = "Transfer"
    const val TRANSFER_OUT = "Transfer out"
    const val TRANSFER_IN = "Transfer in"
}

internal fun DSLContext.ensureAccountOwnedByUser(accountId: UUID, userId: UUID) {
    val exists = fetchExists(
        selectOne().from(ACCOUNTS).where(ACCOUNTS.ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(userId)))
    )
    if (!exists) throw LocalizedException.NotFound("error.account.notFound")
}

/**
 * The inner `SELECT accounts.id WHERE accounts.user_id = :userId` used to fold account ownership into
 * a `TRANSACTIONS.ACCOUNT_ID IN (…)` predicate (ADR-0003). Centralised so the ownership subquery is
 * spelled once instead of re-typed at every bulk delete/update site.
 */
internal fun DSLContext.accountsOwnedBy(userId: UUID): Select<Record1<UUID?>> =
    select(ACCOUNTS.ID).from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(userId))

internal fun DSLContext.findManagedCategoryId(name: String): Long? =
    select(CATEGORIES.ID)
        .from(CATEGORIES)
        .where(CATEGORIES.NAME.eq(name).and(CATEGORIES.IS_MANAGED.isTrue))
        .fetchOne()
        ?.get(CATEGORIES.ID)

internal fun DSLContext.findSystemCategoryId(name: String): Long? =
    select(CATEGORIES.ID)
        .from(CATEGORIES)
        .where(CATEGORIES.NAME.eq(name).and(CATEGORIES.USER_ID.isNull))
        .fetchOne()
        ?.get(CATEGORIES.ID)

/**
 * The ADR-0015 expense-aggregation predicate: EXPENSE rows in [from]..[to] that are NOT a transfer
 * leg (transfer_group_id IS NULL). Centralised so every income/expense aggregate excludes transfer
 * legs from one definition instead of re-spelling the predicate per repository — miss it at one
 * site and reports/budgets silently double-count transfers.
 */
internal fun expenseInPeriod(from: LocalDate, to: LocalDate): Condition =
    TRANSACTIONS.TYPE.eq(CategoryType.EXPENSE.value)
        .and(TRANSACTIONS.TRANSACTION_DATE.between(from, to))
        .and(TRANSACTIONS.TRANSFER_GROUP_ID.isNull)

/**
 * Split-aware effective amount: the split's amount when the transaction is split, else the
 * transaction's own amount. Shared so category/budget/report aggregates agree on one definition.
 */
internal fun effectiveAmount(): Field<BigDecimal?> =
    DSL.coalesce(TRANSACTION_SPLITS.AMOUNT, TRANSACTIONS.AMOUNT)

/** Split-aware effective category id — counterpart to [effectiveAmount]. */
internal fun effectiveCategoryId(): Field<Long?> =
    DSL.coalesce(TRANSACTION_SPLITS.CATEGORY_ID, TRANSACTIONS.CATEGORY_ID)

/** The `to_char(transaction_date, 'YYYY-MM')` month-bucket key used by month-grouped aggregates. */
internal val TXN_MONTH_KEY: Field<String> = DSL.field(
    "to_char({0}, 'YYYY-MM')",
    String::class.java,
    TRANSACTIONS.TRANSACTION_DATE,
)
