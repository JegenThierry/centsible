package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransactionSort
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.BatchImportOutcome
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
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
class TransactionRepository(private val dsl: DSLContext) : ITransactionRepository {
    override fun fetchTransactions(
        accountId: UUID, authenticatedUser: UserDTO, page: Int, pageSize: Int, filters: TransactionFilters
    ): List<TransactionDTO> {
        require(page >= 1) { "page must be >= 1" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
        val offset = (page - 1).toLong() * pageSize

        var condition = baseCondition(accountId, authenticatedUser)
        filters.search?.takeIf { it.isNotBlank() }?.let { term ->
            val escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            condition = condition.and(TRANSACTIONS.DESCRIPTION.likeIgnoreCase("%$escaped%"))
        }
        filters.categoryIds?.takeIf { it.isNotEmpty() }?.let { ids ->
            condition = condition.and(TRANSACTIONS.CATEGORY_ID.`in`(ids))
        }
        filters.from?.let { condition = condition.and(TRANSACTIONS.TRANSACTION_DATE.ge(it)) }
        filters.to?.let { condition = condition.and(TRANSACTIONS.TRANSACTION_DATE.le(it)) }

        val orderBy = when (filters.sort) {
            TransactionSort.DATE_DESC -> arrayOf(TRANSACTIONS.TRANSACTION_DATE.desc(), TRANSACTIONS.ID.desc())
            TransactionSort.DATE_ASC -> arrayOf(TRANSACTIONS.TRANSACTION_DATE.asc(), TRANSACTIONS.ID.asc())
            TransactionSort.AMOUNT_DESC -> arrayOf(TRANSACTIONS.AMOUNT.desc(), TRANSACTIONS.ID.desc())
            TransactionSort.AMOUNT_ASC -> arrayOf(TRANSACTIONS.AMOUNT.asc(), TRANSACTIONS.ID.asc())
        }

        return dsl.select(
            TRANSACTIONS.ID,
            TRANSACTIONS.AMOUNT,
            TRANSACTIONS.DESCRIPTION,
            TRANSACTIONS.TRANSACTION_DATE,
            TRANSACTIONS.CREATED_AT,
            TRANSACTIONS.MODIFIED_AT,
            CATEGORIES.ID,
            CATEGORIES.NAME,
            CATEGORIES.ICON,
            CATEGORIES.TYPE,
            CATEGORIES.COLOR
        ).from(TRANSACTIONS).join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(condition)
            .orderBy(*orderBy).limit(pageSize).offset(offset)
            .fetch { mapToTransactionDTO(it) }
    }

    override fun fetchTransactionById(
        transactionId: UUID, authenticatedUser: UserDTO
    ): TransactionDTO {
        return dsl.select(
            TRANSACTIONS.ID,
            TRANSACTIONS.AMOUNT,
            TRANSACTIONS.DESCRIPTION,
            TRANSACTIONS.TRANSACTION_DATE,
            TRANSACTIONS.CREATED_AT,
            TRANSACTIONS.MODIFIED_AT,
            CATEGORIES.ID,
            CATEGORIES.NAME,
            CATEGORIES.ICON,
            CATEGORIES.TYPE,
            CATEGORIES.COLOR
        ).from(TRANSACTIONS).join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(TRANSACTIONS.ID.eq(transactionId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
            .orderBy(TRANSACTIONS.TRANSACTION_DATE.desc(), TRANSACTIONS.ID.desc())
            .fetchSingle { mapToTransactionDTO(it) }
    }

    private fun mapToTransactionDTO(record: org.jooq.Record): TransactionDTO {
        val category = CategoryDTO(
            id = record[CATEGORIES.ID]!!,
            name = record[CATEGORIES.NAME]!!,
            icon = record[CATEGORIES.ICON]!!,
            type = CategoryType.fromValue(record[CATEGORIES.TYPE]!!),
            color = record[CATEGORIES.COLOR]
        )

        return TransactionDTO(
            id = record[TRANSACTIONS.ID]!!,
            category = category,
            amount = record[TRANSACTIONS.AMOUNT]!!,
            description = record[TRANSACTIONS.DESCRIPTION],
            transactionDate = record[TRANSACTIONS.TRANSACTION_DATE]!!,
            createdAt = record[TRANSACTIONS.CREATED_AT]!!,
            updatedAt = record[TRANSACTIONS.MODIFIED_AT]!!,
        )
    }

    override fun createTransaction(
        accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO
    ): TransactionDTO {
        // INSERT...SELECT WHERE EXISTS: ownership check and insert in one roundtrip.
        val now = OffsetDateTime.now()
        val record = dsl.insertInto(
            TRANSACTIONS,
            TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.CATEGORY_ID, TRANSACTIONS.AMOUNT,
            TRANSACTIONS.DESCRIPTION, TRANSACTIONS.TRANSACTION_DATE,
            TRANSACTIONS.CREATED_AT, TRANSACTIONS.MODIFIED_AT,
        )
            .select(
                dsl.select(
                    DSL.value(accountId), DSL.value(transactionForm.categoryId), DSL.value(transactionForm.amount),
                    DSL.value(transactionForm.description), DSL.value(transactionForm.transactionDate),
                    DSL.value(now), DSL.value(now),
                ).whereExists(
                    dsl.selectOne().from(ACCOUNTS)
                        .where(ACCOUNTS.ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
                )
            )
            .returning(TRANSACTIONS.ID)
            .fetchOne()
            ?: throw IllegalArgumentException("Account not found or not owned by user")

        return fetchTransactionById(record[TRANSACTIONS.ID]!!, authenticatedUser)
    }

    override fun updateTransaction(
        transactionId: UUID, accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO
    ): TransactionDTO {
        // Account subquery ownership-scopes the UPDATE itself (not just the post-fetch).
        val updated = dsl.update(TRANSACTIONS).set(TRANSACTIONS.CATEGORY_ID, transactionForm.categoryId)
            .set(TRANSACTIONS.AMOUNT, transactionForm.amount)
            .set(TRANSACTIONS.DESCRIPTION, transactionForm.description)
            .set(TRANSACTIONS.TRANSACTION_DATE, transactionForm.transactionDate)
            .set(TRANSACTIONS.MODIFIED_AT, OffsetDateTime.now())
            .where(
                TRANSACTIONS.ID.eq(transactionId)
                    .and(TRANSACTIONS.ACCOUNT_ID.eq(accountId))
                    .and(
                        TRANSACTIONS.ACCOUNT_ID.`in`(
                            dsl.select(ACCOUNTS.ID).from(ACCOUNTS)
                                .where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
                        )
                    )
            ).execute()

        if (updated == 0) {
            throw IllegalArgumentException("Transaction not found or not owned by user")
        }

        return fetchTransactionById(transactionId, authenticatedUser)
    }

    override fun deleteTransaction(transactionId: UUID, authenticatedUser: UserDTO): TransactionDTO {
        val transaction = fetchTransactionById(transactionId, authenticatedUser)

        dsl.deleteFrom(TRANSACTIONS).where(TRANSACTIONS.ID.eq(transactionId)).execute()

        return transaction
    }

    override fun fetchTransactionsByIds(
        accountId: UUID, ids: List<UUID>, authenticatedUser: UserDTO
    ): List<TransactionDTO> {
        if (ids.isEmpty()) return emptyList()
        return dsl.select(
            TRANSACTIONS.ID,
            TRANSACTIONS.AMOUNT,
            TRANSACTIONS.DESCRIPTION,
            TRANSACTIONS.TRANSACTION_DATE,
            TRANSACTIONS.CREATED_AT,
            TRANSACTIONS.MODIFIED_AT,
            CATEGORIES.ID,
            CATEGORIES.NAME,
            CATEGORIES.ICON,
            CATEGORIES.TYPE,
            CATEGORIES.COLOR
        ).from(TRANSACTIONS).join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(baseCondition(accountId, authenticatedUser).and(TRANSACTIONS.ID.`in`(ids)))
            .fetch { mapToTransactionDTO(it) }
    }

    override fun deleteTransactions(
        accountId: UUID, ids: List<UUID>, authenticatedUser: UserDTO
    ): Int {
        if (ids.isEmpty()) return 0
        return dsl.deleteFrom(TRANSACTIONS)
            .where(
                TRANSACTIONS.ID.`in`(ids)
                    .and(TRANSACTIONS.ACCOUNT_ID.eq(accountId))
                    .and(
                        TRANSACTIONS.ACCOUNT_ID.`in`(
                            dsl.select(ACCOUNTS.ID).from(ACCOUNTS)
                                .where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
                        )
                    )
            ).execute()
    }

    override fun updateCategoryForTransactions(
        accountId: UUID, ids: List<UUID>, categoryId: Int, authenticatedUser: UserDTO
    ): Int {
        if (ids.isEmpty()) return 0
        return dsl.update(TRANSACTIONS)
            .set(TRANSACTIONS.CATEGORY_ID, categoryId)
            .set(TRANSACTIONS.MODIFIED_AT, OffsetDateTime.now())
            .where(
                TRANSACTIONS.ID.`in`(ids)
                    .and(TRANSACTIONS.ACCOUNT_ID.eq(accountId))
                    .and(
                        TRANSACTIONS.ACCOUNT_ID.`in`(
                            dsl.select(ACCOUNTS.ID).from(ACCOUNTS)
                                .where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
                        )
                    )
            ).execute()
    }

    override fun aggregateByCategory(
        accountId: UUID, authenticatedUser: UserDTO, from: LocalDate, to: LocalDate
    ): List<CategoryAggregateDTO> {
        val total = DSL.sum(TRANSACTIONS.AMOUNT).`as`("total")

        return dsl.select(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.COLOR, CATEGORIES.ICON, total)
            .from(TRANSACTIONS)
            .join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                baseCondition(accountId, authenticatedUser)
                    .and(CATEGORIES.TYPE.eq(CategoryType.EXPENSE.name))
                    .and(TRANSACTIONS.TRANSACTION_DATE.between(from, to))
            )
            .groupBy(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.COLOR, CATEGORIES.ICON)
            .orderBy(total.desc())
            .fetch { record ->
                CategoryAggregateDTO(
                    categoryId = record[CATEGORIES.ID]!!,
                    categoryName = record[CATEGORIES.NAME]!!,
                    categoryColor = record[CATEGORIES.COLOR],
                    categoryIcon = record[CATEGORIES.ICON],
                    total = record[total] ?: BigDecimal.ZERO,
                )
            }
    }

    override fun aggregateByMonth(
        accountId: UUID, authenticatedUser: UserDTO, months: Int
    ): List<MonthlyAggregateDTO> {
        require(months in 1..36) { "months must be between 1 and 36" }
        val today = java.time.LocalDate.now()
        val firstMonth = YearMonth.from(today).minusMonths((months - 1).toLong())
        val start = firstMonth.atDay(1)

        val monthExpr = DSL.field("to_char({0}, 'YYYY-MM')", String::class.java, TRANSACTIONS.TRANSACTION_DATE).`as`("ym")
        val incomeExpr = DSL.sum(
            DSL.case_().`when`(CATEGORIES.TYPE.eq(CategoryType.INCOME.name), TRANSACTIONS.AMOUNT)
                .otherwise(BigDecimal.ZERO)
        ).`as`("income")
        val expenseExpr = DSL.sum(
            DSL.case_().`when`(CATEGORIES.TYPE.eq(CategoryType.EXPENSE.name), TRANSACTIONS.AMOUNT)
                .otherwise(BigDecimal.ZERO)
        ).`as`("expense")

        val rows = dsl.select(monthExpr, incomeExpr, expenseExpr)
            .from(TRANSACTIONS)
            .join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                baseCondition(accountId, authenticatedUser)
                    .and(TRANSACTIONS.TRANSACTION_DATE.ge(start))
            )
            .groupBy(monthExpr)
            .fetch { record ->
                record[monthExpr]!! to MonthlyAggregateDTO(
                    yearMonth = record[monthExpr]!!,
                    income = record[incomeExpr] ?: BigDecimal.ZERO,
                    expense = record[expenseExpr] ?: BigDecimal.ZERO,
                )
            }.toMap()

        return (0 until months).map { offset ->
            val ym = firstMonth.plusMonths(offset.toLong()).toString()
            rows[ym] ?: MonthlyAggregateDTO(yearMonth = ym)
        }
    }

    override fun importBatch(
        accountId: UUID,
        rows: List<ImportTransactionRow>,
        hashes: List<String>,
        authenticatedUser: UserDTO,
    ): BatchImportOutcome {
        require(rows.size == hashes.size) { "rows and hashes must have equal length" }
        if (rows.isEmpty()) return BatchImportOutcome(0, BigDecimal.ZERO)

        val ownsAccount = dsl.selectOne().from(ACCOUNTS)
            .where(ACCOUNTS.ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
            .fetchOne() != null
        if (!ownsAccount) throw IllegalArgumentException("Account not found or not owned by user")

        val now = OffsetDateTime.now()
        val insertStep = dsl.insertInto(
            TRANSACTIONS,
            TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.CATEGORY_ID, TRANSACTIONS.AMOUNT,
            TRANSACTIONS.DESCRIPTION, TRANSACTIONS.TRANSACTION_DATE, TRANSACTIONS.IMPORT_HASH,
            TRANSACTIONS.CREATED_AT, TRANSACTIONS.MODIFIED_AT,
        )
        rows.forEachIndexed { i, row ->
            insertStep.values(
                accountId, row.categoryId, row.amount,
                row.description, row.transactionDate, hashes[i],
                now, now,
            )
        }

        val inserted = insertStep
            .onConflict(TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.IMPORT_HASH)
            .where(TRANSACTIONS.IMPORT_HASH.isNotNull)
            .doNothing()
            .returning(TRANSACTIONS.CATEGORY_ID, TRANSACTIONS.AMOUNT)
            .fetch()

        if (inserted.isEmpty()) return BatchImportOutcome(0, BigDecimal.ZERO)

        val categoryIds = inserted.map { it[TRANSACTIONS.CATEGORY_ID]!! }.distinct()
        val typeByCategoryId = dsl.select(CATEGORIES.ID, CATEGORIES.TYPE)
            .from(CATEGORIES)
            .where(CATEGORIES.ID.`in`(categoryIds))
            .fetch { it[CATEGORIES.ID]!! to CategoryType.fromValue(it[CATEGORIES.TYPE]!!) }
            .toMap()

        val net = inserted.fold(BigDecimal.ZERO) { acc, rec ->
            val type = typeByCategoryId[rec[TRANSACTIONS.CATEGORY_ID]!!]
            val amount = rec[TRANSACTIONS.AMOUNT]!!
            acc + if (type == CategoryType.INCOME) amount else amount.negate()
        }

        return BatchImportOutcome(insertedCount = inserted.size, netBalanceAdjustment = net)
    }

    private fun baseCondition(accountId: UUID, authenticatedUser: UserDTO) =
        TRANSACTIONS.ACCOUNT_ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
}
