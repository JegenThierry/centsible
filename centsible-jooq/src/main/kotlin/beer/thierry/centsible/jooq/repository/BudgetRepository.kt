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

    private val periodTypeField = DSL.field("period_type", String::class.java)
    private val rolloverField = DSL.field("rollover_enabled", Boolean::class.java)

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
            periodTypeField,
            rolloverField,
        )
            .from(BUDGETS)
            .join(CATEGORIES).on(CATEGORIES.ID.eq(BUDGETS.CATEGORY_ID))
            .where(BUDGETS.USER_ID.eq(authenticatedUser.id))
            .orderBy(CATEGORIES.NAME.asc())
            .fetch()

        return rows.map { record ->
            val periodType = parsePeriodType(record[periodTypeField])
            val window = periodWindow(periodType, yearMonth)
            val spent = sumExpenses(authenticatedUser, record[BUDGETS.CATEGORY_ID]!!, window.first, window.second)
            val rollover = if (record[rolloverField] == true) {
                computeRollover(
                    authenticatedUser,
                    record[BUDGETS.CATEGORY_ID]!!,
                    record[BUDGETS.AMOUNT_LIMIT]!!,
                    periodType,
                    yearMonth,
                )
            } else BigDecimal.ZERO

            mapToDTO(record, periodKeyFor(periodType, yearMonth), spent, periodType, record[rolloverField] == true, rollover)
        }
    }

    private fun sumExpenses(user: UserDTO, categoryId: Long, from: LocalDate, to: LocalDate): BigDecimal {
        val total = dsl.select(DSL.sum(TRANSACTIONS.AMOUNT))
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                TRANSACTIONS.CATEGORY_ID.eq(categoryId)
                    .and(ACCOUNTS.USER_ID.eq(user.id))
                    .and(TRANSACTIONS.TRANSACTION_DATE.between(from, to))
            )
            .fetchOne(0, BigDecimal::class.java)
        return total ?: BigDecimal.ZERO
    }

    private fun computeRollover(
        user: UserDTO,
        categoryId: Long,
        currentLimit: BigDecimal,
        periodType: BudgetPeriodType,
        yearMonth: YearMonth,
    ): BigDecimal {
        val previous = previousPeriod(periodType, yearMonth)
        val prevSpent = sumExpenses(user, categoryId, previous.first, previous.second)
        val leftover = currentLimit.subtract(prevSpent)
        return if (leftover.signum() > 0) leftover else BigDecimal.ZERO
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
            periodTypeField,
            rolloverField,
        )
            .from(BUDGETS)
            .join(CATEGORIES).on(CATEGORIES.ID.eq(BUDGETS.CATEGORY_ID))
            .where(BUDGETS.ID.eq(id).and(BUDGETS.USER_ID.eq(authenticatedUser.id)))
            .fetchSingle { record ->
                val pt = parsePeriodType(record[periodTypeField])
                mapToDTO(record, null, BigDecimal.ZERO, pt, record[rolloverField] == true, BigDecimal.ZERO)
            }
    }

    override fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO {
        val now = OffsetDateTime.now()
        val record = dsl.insertInto(BUDGETS)
            .set(BUDGETS.USER_ID, authenticatedUser.id)
            .set(BUDGETS.CATEGORY_ID, form.categoryId)
            .set(BUDGETS.AMOUNT_LIMIT, form.amountLimit)
            .set(BUDGETS.CREATED_AT, now)
            .set(BUDGETS.MODIFIED_AT, now)
            .set(periodTypeField, form.periodType.name)
            .set(rolloverField, form.rolloverEnabled)
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
            .set(periodTypeField, form.periodType.name)
            .set(rolloverField, form.rolloverEnabled)
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

    private fun parsePeriodType(raw: String?): BudgetPeriodType = try {
        BudgetPeriodType.valueOf(raw ?: "MONTHLY")
    } catch (_: IllegalArgumentException) {
        BudgetPeriodType.MONTHLY
    }

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
