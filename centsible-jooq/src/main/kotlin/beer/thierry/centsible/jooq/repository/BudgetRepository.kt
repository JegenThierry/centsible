package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
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
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.*

@Repository
class BudgetRepository(private val dsl: DSLContext) : IBudgetRepository {

    override fun fetchAllWithSpentForMonth(authenticatedUser: UserDTO, yearMonth: YearMonth): List<BudgetDTO> {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val spentSubquery = DSL.coalesce(
            DSL.select(DSL.sum(TRANSACTIONS.AMOUNT))
                .from(TRANSACTIONS)
                .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
                .where(
                    TRANSACTIONS.CATEGORY_ID.eq(BUDGETS.CATEGORY_ID)
                        .and(ACCOUNTS.USER_ID.eq(BUDGETS.USER_ID))
                        .and(TRANSACTIONS.TRANSACTION_DATE.between(startDate, endDate))
                ),
            BigDecimal.ZERO,
        ).`as`("spent")

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
            spentSubquery,
        )
            .from(BUDGETS)
            .join(CATEGORIES).on(CATEGORIES.ID.eq(BUDGETS.CATEGORY_ID))
            .where(BUDGETS.USER_ID.eq(authenticatedUser.id))
            .orderBy(CATEGORIES.NAME.asc())
            .fetch { record ->
                val spent = record.get(spentSubquery) as? BigDecimal ?: BigDecimal.ZERO
                mapToDTO(record, BudgetDTO.periodKey(yearMonth), spent)
            }
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
        )
            .from(BUDGETS)
            .join(CATEGORIES).on(CATEGORIES.ID.eq(BUDGETS.CATEGORY_ID))
            .where(BUDGETS.ID.eq(id).and(BUDGETS.USER_ID.eq(authenticatedUser.id)))
            .fetchSingle { record -> mapToDTO(record, null, BigDecimal.ZERO) }
    }

    override fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO {
        val now = OffsetDateTime.now()
        val record = dsl.insertInto(
            BUDGETS,
            BUDGETS.USER_ID, BUDGETS.CATEGORY_ID, BUDGETS.AMOUNT_LIMIT,
            BUDGETS.CREATED_AT, BUDGETS.MODIFIED_AT,
        )
            .values(authenticatedUser.id, form.categoryId, form.amountLimit, now, now)
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

    private fun mapToDTO(record: org.jooq.Record, period: String?, spent: BigDecimal?): BudgetDTO {
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
            createdAt = record[BUDGETS.CREATED_AT]!!,
            updatedAt = record[BUDGETS.MODIFIED_AT]!!,
        )
    }
}
