package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.budget.BudgetPeriodType
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.BUDGETS
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import beer.thierry.jooq.generated.tables.references.TRANSACTION_SPLITS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.*

@Repository
class BudgetRepository(private val dsl: DSLContext) : IBudgetRepository {

    override fun fetchAllWithSpentForMonth(authenticatedUser: UserDTO, yearMonth: YearMonth): List<BudgetDTO> {
        val rows = dsl.select(
            BUDGETS.ID,
            BUDGETS.AMOUNT_LIMIT,
            BUDGETS.CREATED_AT,
            BUDGETS.MODIFIED_AT,
            BUDGETS.CATEGORY_ID,
            CATEGORIES.ID,
            CATEGORIES.NAME,
            CATEGORIES.ICON,
            CATEGORIES.TYPE,
            CATEGORIES.COLOR,
            BUDGETS.PERIOD_TYPE,
            BUDGETS.ROLLOVER_ENABLED,
        )
            .from(BUDGETS)
            .join(CATEGORIES).on(
                CATEGORIES.ID.eq(BUDGETS.CATEGORY_ID)
                    .and(CATEGORIES.USER_ID.eq(authenticatedUser.id).or(CATEGORIES.USER_ID.isNull))
            )
            .where(BUDGETS.USER_ID.eq(authenticatedUser.id))
            .orderBy(CATEGORIES.NAME.asc())
            .fetch()

        val byType = rows.groupBy { parsePeriodType(it[BUDGETS.PERIOD_TYPE]) }
        val currentSums = mutableMapOf<Long, BigDecimal>()
        val previousSums = mutableMapOf<Long, BigDecimal>()

        for ((periodType, group) in byType) {
            val categoryIds = group.map { it[BUDGETS.CATEGORY_ID]!! }
            val (curFrom, curTo) = periodWindow(periodType, yearMonth)
            currentSums.putAll(sumByCategory(authenticatedUser, categoryIds, curFrom, curTo))

            if (group.any { it[BUDGETS.ROLLOVER_ENABLED] == true }) {
                val (prevFrom, prevTo) = previousPeriod(periodType, yearMonth)
                previousSums.putAll(sumByCategory(authenticatedUser, categoryIds, prevFrom, prevTo))
            }
        }

        return rows.map { record ->
            val periodType = parsePeriodType(record[BUDGETS.PERIOD_TYPE])
            val categoryId = record[BUDGETS.CATEGORY_ID]!!
            val spent = currentSums[categoryId] ?: BigDecimal.ZERO
            val rolloverEnabled = record[BUDGETS.ROLLOVER_ENABLED] == true
            val rollover = if (rolloverEnabled) {
                val leftover = record[BUDGETS.AMOUNT_LIMIT]!!.subtract(previousSums[categoryId] ?: BigDecimal.ZERO)
                if (leftover.signum() > 0) leftover else BigDecimal.ZERO
            } else BigDecimal.ZERO

            mapToDTO(record, periodKeyFor(periodType, yearMonth), spent, periodType, rolloverEnabled, rollover)
        }
    }

    private fun sumByCategory(
        user: UserDTO,
        categoryIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
    ): Map<Long, BigDecimal> {
        if (categoryIds.isEmpty()) return emptyMap()
        val effectiveCategoryId = DSL.coalesce(TRANSACTION_SPLITS.CATEGORY_ID, TRANSACTIONS.CATEGORY_ID)
        val effectiveAmount = DSL.coalesce(TRANSACTION_SPLITS.AMOUNT, TRANSACTIONS.AMOUNT)
        val categoryKey = effectiveCategoryId.`as`("category_id")
        val total = DSL.sum(effectiveAmount).`as`("total")
        return dsl.select(categoryKey, total)
            .from(TRANSACTIONS)
            .leftJoin(TRANSACTION_SPLITS).on(TRANSACTION_SPLITS.TRANSACTION_ID.eq(TRANSACTIONS.ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                effectiveCategoryId.`in`(categoryIds)
                    .and(ACCOUNTS.USER_ID.eq(user.id))
                    .and(expenseInPeriod(from, to))
            )
            .groupBy(effectiveCategoryId)
            .fetch()
            .associate { it[categoryKey]!! to (it[total] ?: BigDecimal.ZERO) }
    }

    override fun fetchById(id: UUID, authenticatedUser: UserDTO): BudgetDTO {
        return dsl.select(
            BUDGETS.ID,
            BUDGETS.AMOUNT_LIMIT,
            BUDGETS.CREATED_AT,
            BUDGETS.MODIFIED_AT,
            CATEGORIES.ID,
            CATEGORIES.NAME,
            CATEGORIES.ICON,
            CATEGORIES.TYPE,
            CATEGORIES.COLOR,
            BUDGETS.PERIOD_TYPE,
            BUDGETS.ROLLOVER_ENABLED,
        )
            .from(BUDGETS)
            .join(CATEGORIES).on(
                CATEGORIES.ID.eq(BUDGETS.CATEGORY_ID)
                    .and(CATEGORIES.USER_ID.eq(authenticatedUser.id).or(CATEGORIES.USER_ID.isNull))
            )
            .where(BUDGETS.ID.eq(id).and(BUDGETS.USER_ID.eq(authenticatedUser.id)))
            .fetchSingle { record ->
                val pt = parsePeriodType(record[BUDGETS.PERIOD_TYPE])
                mapToDTO(record, null, BigDecimal.ZERO, pt, record[BUDGETS.ROLLOVER_ENABLED] == true, BigDecimal.ZERO)
            }
    }

    override fun existsForCategoryAndPeriod(
        authenticatedUser: UserDTO,
        categoryId: Long,
        periodType: BudgetPeriodType,
        excludeBudgetId: UUID?,
    ): Boolean {
        var condition = BUDGETS.USER_ID.eq(authenticatedUser.id)
            .and(BUDGETS.CATEGORY_ID.eq(categoryId))
            .and(BUDGETS.PERIOD_TYPE.eq(periodType.name))
        if (excludeBudgetId != null) condition = condition.and(BUDGETS.ID.ne(excludeBudgetId))
        return dsl.fetchExists(dsl.selectOne().from(BUDGETS).where(condition))
    }

    override fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO {
        val now = OffsetDateTime.now()
        val record = dsl.insertInto(BUDGETS)
            .set(BUDGETS.USER_ID, authenticatedUser.id)
            .set(BUDGETS.CATEGORY_ID, form.categoryId)
            .set(BUDGETS.AMOUNT_LIMIT, form.amountLimit)
            .set(BUDGETS.CREATED_AT, now)
            .set(BUDGETS.MODIFIED_AT, now)
            .set(BUDGETS.PERIOD_TYPE, form.periodType.name)
            .set(BUDGETS.ROLLOVER_ENABLED, form.rolloverEnabled)
            .returning(BUDGETS.ID)
            .fetchOne()
            ?: throw IllegalStateException("Failed to insert budget")

        return fetchById(record[BUDGETS.ID]!!, authenticatedUser)
    }

    override fun update(id: UUID, form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO {
        val updated = dsl.update(BUDGETS)
            .set(BUDGETS.CATEGORY_ID, form.categoryId)
            .set(BUDGETS.AMOUNT_LIMIT, form.amountLimit)
            .set(BUDGETS.MODIFIED_AT, OffsetDateTime.now())
            .set(BUDGETS.PERIOD_TYPE, form.periodType.name)
            .set(BUDGETS.ROLLOVER_ENABLED, form.rolloverEnabled)
            .where(BUDGETS.ID.eq(id).and(BUDGETS.USER_ID.eq(authenticatedUser.id)))
            .execute()
        if (updated == 0) throw IllegalArgumentException("Budget not found or not owned by user")
        return fetchById(id, authenticatedUser)
    }

    override fun delete(id: UUID, authenticatedUser: UserDTO) {
        val deleted = dsl.deleteFrom(BUDGETS)
            .where(BUDGETS.ID.eq(id).and(BUDGETS.USER_ID.eq(authenticatedUser.id)))
            .execute()
        if (deleted == 0) throw IllegalArgumentException("Budget not found or not owned by user")
    }

    private fun parsePeriodType(raw: String?): BudgetPeriodType =
        BudgetPeriodType.valueOf(raw ?: BudgetPeriodType.MONTHLY.name)

    private fun periodWindow(type: BudgetPeriodType, ym: YearMonth): Pair<LocalDate, LocalDate> = when (type) {
        BudgetPeriodType.MONTHLY -> ym.atDay(1) to ym.atEndOfMonth()
        BudgetPeriodType.QUARTERLY -> {
            val quarter = (ym.monthValue - 1) / 3
            val startMonth = quarter * 3 + 1
            val start = YearMonth.of(ym.year, startMonth)
            val end = start.plusMonths(2)
            start.atDay(1) to end.atEndOfMonth()
        }
        BudgetPeriodType.ANNUAL -> LocalDate.of(ym.year, 1, 1) to LocalDate.of(ym.year, 12, 31)
    }

    private fun previousPeriod(type: BudgetPeriodType, ym: YearMonth): Pair<LocalDate, LocalDate> = when (type) {
        BudgetPeriodType.MONTHLY -> periodWindow(type, ym.minusMonths(1))
        BudgetPeriodType.QUARTERLY -> periodWindow(type, ym.minusMonths(3))
        BudgetPeriodType.ANNUAL -> periodWindow(type, ym.minusYears(1))
    }

    private fun periodKeyFor(type: BudgetPeriodType, ym: YearMonth): String = when (type) {
        BudgetPeriodType.MONTHLY -> ym.toString()
        BudgetPeriodType.QUARTERLY -> {
            val quarter = (ym.monthValue - 1) / 3 + 1
            "${ym.year}-Q$quarter"
        }
        BudgetPeriodType.ANNUAL -> ym.year.toString()
    }

    private fun mapToDTO(
        record: org.jooq.Record,
        period: String?,
        spent: BigDecimal?,
        periodType: BudgetPeriodType,
        rolloverEnabled: Boolean,
        rolloverAmount: BigDecimal,
    ): BudgetDTO {
        val category = CategoryDTO(
            id = record[CATEGORIES.ID]!!,
            name = record[CATEGORIES.NAME]!!,
            icon = record[CATEGORIES.ICON]!!,
            type = CategoryType.fromValue(record[CATEGORIES.TYPE]!!),
            color = record[CATEGORIES.COLOR],
        )
        return BudgetDTO(
            id = record[BUDGETS.ID]!!,
            category = category,
            amountLimit = record[BUDGETS.AMOUNT_LIMIT]!!,
            amountSpent = spent ?: BigDecimal.ZERO,
            period = period,
            periodType = periodType,
            rolloverEnabled = rolloverEnabled,
            rolloverAmount = rolloverAmount,
            createdAt = record[BUDGETS.CREATED_AT]!!,
            updatedAt = record[BUDGETS.MODIFIED_AT]!!,
        )
    }
}
