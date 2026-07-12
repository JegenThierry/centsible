package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.transaction.CategoryAggregateDTO
import beer.thierry.centsible.api.model.transaction.DailyAggregateDTO
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.api.model.transaction.MonthlyAggregateDTO
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.transaction.TransactionFilters
import beer.thierry.centsible.api.model.transaction.TransactionForm
import beer.thierry.centsible.api.model.transaction.TransactionSort
import beer.thierry.centsible.api.model.transaction.TransactionSplitDTO
import beer.thierry.centsible.api.model.transaction.TransactionSplitForm
import beer.thierry.centsible.api.model.transaction.TransferForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.BatchImportOutcome
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.repository.RuleCandidateTransaction
import beer.thierry.centsible.api.repository.TransferLeg
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
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
            condition = condition.and(
                TRANSACTIONS.CATEGORY_ID.`in`(ids).or(
                    DSL.exists(
                        dsl.selectOne().from(TRANSACTION_SPLITS)
                            .where(
                                TRANSACTION_SPLITS.TRANSACTION_ID.eq(TRANSACTIONS.ID)
                                    .and(TRANSACTION_SPLITS.CATEGORY_ID.`in`(ids))
                            )
                    )
                )
            )
        }
        filters.from?.let { condition = condition.and(TRANSACTIONS.TRANSACTION_DATE.ge(it)) }
        filters.to?.let { condition = condition.and(TRANSACTIONS.TRANSACTION_DATE.le(it)) }
        filters.type?.let { condition = condition.and(TRANSACTIONS.TYPE.eq(it.value)) }
        filters.amountMin?.let { condition = condition.and(TRANSACTIONS.AMOUNT.ge(it)) }
        filters.amountMax?.let { condition = condition.and(TRANSACTIONS.AMOUNT.le(it)) }

        val orderBy = when (filters.sort) {
            TransactionSort.DATE_DESC -> arrayOf(TRANSACTIONS.TRANSACTION_DATE.desc(), TRANSACTIONS.ID.desc())
            TransactionSort.DATE_ASC -> arrayOf(TRANSACTIONS.TRANSACTION_DATE.asc(), TRANSACTIONS.ID.asc())
            TransactionSort.AMOUNT_DESC -> arrayOf(TRANSACTIONS.AMOUNT.desc(), TRANSACTIONS.ID.desc())
            TransactionSort.AMOUNT_ASC -> arrayOf(TRANSACTIONS.AMOUNT.asc(), TRANSACTIONS.ID.asc())
        }

        return dsl.select(*TransactionRecordMapper.columns)
            .from(TRANSACTIONS).join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(condition)
            .orderBy(*orderBy).limit(pageSize).offset(offset)
            .fetch { TransactionRecordMapper.mapTransaction(it) }
    }

    override fun fetchTransactionById(
        transactionId: UUID, authenticatedUser: UserDTO
    ): TransactionDTO {
        return dsl.select(*TransactionRecordMapper.columns)
            .from(TRANSACTIONS).join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(TRANSACTIONS.ID.eq(transactionId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
            .orderBy(TRANSACTIONS.TRANSACTION_DATE.desc(), TRANSACTIONS.ID.desc())
            .fetchOne { TransactionRecordMapper.mapTransaction(it) }
            ?: throw LocalizedException.NotFound("error.transaction.notFound")
    }

    override fun createTransaction(
        accountId: UUID, transactionForm: TransactionForm, conversion: ConversionResult, authenticatedUser: UserDTO
    ): TransactionDTO {
        val type = transactionForm.type
            ?: throw LocalizedException.BadRequest("error.transaction.typeRequired")
        val fx = FxColumns.from(conversion)

        val now = OffsetDateTime.now()
        val record = dsl.insertInto(
            TRANSACTIONS,
            TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.CATEGORY_ID, TRANSACTIONS.AMOUNT,
            TRANSACTIONS.DESCRIPTION, TRANSACTIONS.TRANSACTION_DATE, TRANSACTIONS.TYPE,
            TRANSACTIONS.ORIGINAL_AMOUNT, TRANSACTIONS.ORIGINAL_CURRENCY,
            TRANSACTIONS.EXCHANGE_RATE, TRANSACTIONS.RATE_DATE,
            TRANSACTIONS.CREATED_AT, TRANSACTIONS.MODIFIED_AT,
        )
            .select(
                dsl.select(
                    DSL.value(accountId), DSL.value(transactionForm.categoryId), DSL.value(conversion.convertedAmount),
                    DSL.value(transactionForm.description), DSL.value(transactionForm.transactionDate),
                    DSL.value(type.value),
                    DSL.value(fx.originalAmount, TRANSACTIONS.ORIGINAL_AMOUNT),
                    DSL.value(fx.originalCurrency, TRANSACTIONS.ORIGINAL_CURRENCY),
                    DSL.value(fx.rate, TRANSACTIONS.EXCHANGE_RATE),
                    DSL.value(fx.rateDate, TRANSACTIONS.RATE_DATE),
                    DSL.value(now), DSL.value(now),
                ).whereExists(
                    dsl.selectOne().from(ACCOUNTS)
                        .where(ACCOUNTS.ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
                )
            )
            .returning(TRANSACTIONS.ID)
            .fetchOne()
            ?: throw LocalizedException.NotFound("error.account.notFound")

        return fetchTransactionById(record[TRANSACTIONS.ID]!!, authenticatedUser)
    }

    override fun updateTransaction(
        transactionId: UUID, accountId: UUID, transactionForm: TransactionForm, conversion: ConversionResult,
        authenticatedUser: UserDTO
    ): TransactionDTO {
        val type = transactionForm.type
            ?: throw LocalizedException.BadRequest("error.transaction.typeRequired")
        val fx = FxColumns.from(conversion)

        val updated = dsl.update(TRANSACTIONS).set(TRANSACTIONS.CATEGORY_ID, transactionForm.categoryId)
            .set(TRANSACTIONS.AMOUNT, conversion.convertedAmount)
            .set(TRANSACTIONS.DESCRIPTION, transactionForm.description)
            .set(TRANSACTIONS.TRANSACTION_DATE, transactionForm.transactionDate)
            .set(TRANSACTIONS.TYPE, type.value)
            .set(TRANSACTIONS.ORIGINAL_AMOUNT, fx.originalAmount)
            .set(TRANSACTIONS.ORIGINAL_CURRENCY, fx.originalCurrency)
            .set(TRANSACTIONS.EXCHANGE_RATE, fx.rate)
            .set(TRANSACTIONS.RATE_DATE, fx.rateDate)
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
            throw LocalizedException.NotFound("error.transaction.notFound")
        }

        return fetchTransactionById(transactionId, authenticatedUser)
    }

    override fun deleteTransaction(transactionId: UUID, accountId: UUID, authenticatedUser: UserDTO): TransactionDTO {
        val transaction = fetchTransactionById(transactionId, authenticatedUser)

        val deleted = dsl.deleteFrom(TRANSACTIONS)
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

        if (deleted == 0) {
            throw LocalizedException.NotFound("error.transaction.notFound")
        }

        return transaction
    }

    override fun insertTransfer(
        sourceAccountId: UUID,
        destinationAccountId: UUID,
        form: TransferForm,
        conversion: ConversionResult,
        authenticatedUser: UserDTO,
    ): List<TransactionDTO> {
        dsl.ensureAccountOwnedByUser(sourceAccountId, authenticatedUser.id)
        dsl.ensureAccountOwnedByUser(destinationAccountId, authenticatedUser.id)

        val outCategoryId = dsl.findManagedCategoryId(ManagedCategoryNames.TRANSFER_OUT)
            ?: throw IllegalStateException("System 'Transfer out' category not found. Migration may not have run.")
        val inCategoryId = dsl.findManagedCategoryId(ManagedCategoryNames.TRANSFER_IN)
            ?: throw IllegalStateException("System 'Transfer in' category not found. Migration may not have run.")

        val groupId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        val destFx = FxColumns.from(conversion)

        val outId = dsl.insertInto(TRANSACTIONS)
            .set(TRANSACTIONS.ACCOUNT_ID, sourceAccountId)
            .set(TRANSACTIONS.CATEGORY_ID, outCategoryId)
            .set(TRANSACTIONS.AMOUNT, form.amount)
            .set(TRANSACTIONS.DESCRIPTION, form.description)
            .set(TRANSACTIONS.TRANSACTION_DATE, form.transactionDate)
            .set(TRANSACTIONS.TYPE, CategoryType.EXPENSE.value)
            .set(TRANSACTIONS.TRANSFER_GROUP_ID, groupId)
            .set(TRANSACTIONS.CREATED_AT, now)
            .set(TRANSACTIONS.MODIFIED_AT, now)
            .returning(TRANSACTIONS.ID)
            .fetchOne()
            ?.get(TRANSACTIONS.ID)
            ?: throw IllegalStateException("Failed to create transfer source leg")

        val inId = dsl.insertInto(TRANSACTIONS)
            .set(TRANSACTIONS.ACCOUNT_ID, destinationAccountId)
            .set(TRANSACTIONS.CATEGORY_ID, inCategoryId)
            .set(TRANSACTIONS.AMOUNT, conversion.convertedAmount)
            .set(TRANSACTIONS.DESCRIPTION, form.description)
            .set(TRANSACTIONS.TRANSACTION_DATE, form.transactionDate)
            .set(TRANSACTIONS.TYPE, CategoryType.INCOME.value)
            .set(TRANSACTIONS.ORIGINAL_AMOUNT, destFx.originalAmount)
            .set(TRANSACTIONS.ORIGINAL_CURRENCY, destFx.originalCurrency)
            .set(TRANSACTIONS.EXCHANGE_RATE, destFx.rate)
            .set(TRANSACTIONS.RATE_DATE, destFx.rateDate)
            .set(TRANSACTIONS.TRANSFER_GROUP_ID, groupId)
            .set(TRANSACTIONS.CREATED_AT, now)
            .set(TRANSACTIONS.MODIFIED_AT, now)
            .returning(TRANSACTIONS.ID)
            .fetchOne()
            ?.get(TRANSACTIONS.ID)
            ?: throw IllegalStateException("Failed to create transfer destination leg")

        return listOf(
            fetchTransactionById(outId, authenticatedUser),
            fetchTransactionById(inId, authenticatedUser),
        )
    }

    override fun updateTransfer(
        sourceLegId: UUID,
        destinationLegId: UUID,
        sourceAccountId: UUID,
        destinationAccountId: UUID,
        form: TransferForm,
        conversion: ConversionResult,
        authenticatedUser: UserDTO,
    ): List<TransactionDTO> {
        dsl.ensureAccountOwnedByUser(sourceAccountId, authenticatedUser.id)
        dsl.ensureAccountOwnedByUser(destinationAccountId, authenticatedUser.id)

        val now = OffsetDateTime.now()
        val destFx = FxColumns.from(conversion)

        val outUpdated = dsl.update(TRANSACTIONS)
            .set(TRANSACTIONS.ACCOUNT_ID, sourceAccountId)
            .set(TRANSACTIONS.AMOUNT, form.amount)
            .set(TRANSACTIONS.DESCRIPTION, form.description)
            .set(TRANSACTIONS.TRANSACTION_DATE, form.transactionDate)
            .set(TRANSACTIONS.ORIGINAL_AMOUNT, null as BigDecimal?)
            .set(TRANSACTIONS.ORIGINAL_CURRENCY, null as String?)
            .set(TRANSACTIONS.EXCHANGE_RATE, null as BigDecimal?)
            .set(TRANSACTIONS.RATE_DATE, null as LocalDate?)
            .set(TRANSACTIONS.MODIFIED_AT, now)
            .where(transferLegOwnershipCondition(sourceLegId, authenticatedUser))
            .execute()

        val inUpdated = dsl.update(TRANSACTIONS)
            .set(TRANSACTIONS.ACCOUNT_ID, destinationAccountId)
            .set(TRANSACTIONS.AMOUNT, conversion.convertedAmount)
            .set(TRANSACTIONS.DESCRIPTION, form.description)
            .set(TRANSACTIONS.TRANSACTION_DATE, form.transactionDate)
            .set(TRANSACTIONS.ORIGINAL_AMOUNT, destFx.originalAmount)
            .set(TRANSACTIONS.ORIGINAL_CURRENCY, destFx.originalCurrency)
            .set(TRANSACTIONS.EXCHANGE_RATE, destFx.rate)
            .set(TRANSACTIONS.RATE_DATE, destFx.rateDate)
            .set(TRANSACTIONS.MODIFIED_AT, now)
            .where(transferLegOwnershipCondition(destinationLegId, authenticatedUser))
            .execute()

        if (outUpdated == 0 || inUpdated == 0) {
            throw LocalizedException.NotFound("error.transaction.notFound")
        }

        return listOf(
            fetchTransactionById(sourceLegId, authenticatedUser),
            fetchTransactionById(destinationLegId, authenticatedUser),
        )
    }

    private fun transferLegOwnershipCondition(legId: UUID, authenticatedUser: UserDTO) =
        TRANSACTIONS.ID.eq(legId).and(
            TRANSACTIONS.ACCOUNT_ID.`in`(
                dsl.select(ACCOUNTS.ID).from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
            )
        )

    override fun fetchTransferLegs(transferGroupId: UUID, authenticatedUser: UserDTO): List<TransferLeg> =
        dsl.select(TRANSACTIONS.ID, TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.TYPE, TRANSACTIONS.AMOUNT)
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                TRANSACTIONS.TRANSFER_GROUP_ID.eq(transferGroupId)
                    .and(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
            )
            .fetch { record ->
                TransferLeg(
                    id = record[TRANSACTIONS.ID]!!,
                    accountId = record[TRANSACTIONS.ACCOUNT_ID]!!,
                    type = CategoryType.fromValue(record[TRANSACTIONS.TYPE]!!),
                    amount = record[TRANSACTIONS.AMOUNT]!!,
                )
            }

    override fun deleteTransactionsByIds(ids: List<UUID>, authenticatedUser: UserDTO): Int {
        if (ids.isEmpty()) return 0
        return dsl.deleteFrom(TRANSACTIONS)
            .where(
                TRANSACTIONS.ID.`in`(ids)
                    .and(
                        TRANSACTIONS.ACCOUNT_ID.`in`(
                            dsl.select(ACCOUNTS.ID).from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
                        )
                    )
            )
            .execute()
    }

    override fun fetchTransactionsByIds(
        accountId: UUID, ids: List<UUID>, authenticatedUser: UserDTO
    ): List<TransactionDTO> {
        if (ids.isEmpty()) return emptyList()
        return dsl.select(*TransactionRecordMapper.columns)
            .from(TRANSACTIONS).join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(baseCondition(accountId, authenticatedUser).and(TRANSACTIONS.ID.`in`(ids)))
            .fetch { TransactionRecordMapper.mapTransaction(it) }
    }

    override fun deleteTransactions(
        accountId: UUID, ids: List<UUID>, authenticatedUser: UserDTO
    ): Int {
        if (ids.isEmpty()) return 0
        return dsl.deleteFrom(TRANSACTIONS)
            .where(bulkOwnershipCondition(accountId, ids, authenticatedUser))
            .execute()
    }

    override fun updateCategoryForTransactions(
        accountId: UUID, ids: List<UUID>, categoryId: Long, authenticatedUser: UserDTO
    ): Int {
        if (ids.isEmpty()) return 0
        return dsl.update(TRANSACTIONS)
            .set(TRANSACTIONS.CATEGORY_ID, categoryId)
            .set(TRANSACTIONS.MODIFIED_AT, OffsetDateTime.now())
            .where(bulkOwnershipCondition(accountId, ids, authenticatedUser))
            .execute()
    }

    private fun bulkOwnershipCondition(accountId: UUID, ids: List<UUID>, user: UserDTO) =
        TRANSACTIONS.ID.`in`(ids)
            .and(TRANSACTIONS.ACCOUNT_ID.eq(accountId))
            .and(
                TRANSACTIONS.ACCOUNT_ID.`in`(
                    dsl.select(ACCOUNTS.ID).from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(user.id))
                )
            )

    override fun aggregateByCategory(
        accountId: UUID, authenticatedUser: UserDTO, from: LocalDate, to: LocalDate
    ): List<CategoryAggregateDTO> {
        val effectiveAmount = DSL.coalesce(TRANSACTION_SPLITS.AMOUNT, TRANSACTIONS.AMOUNT)
        val effectiveCategoryId = DSL.coalesce(TRANSACTION_SPLITS.CATEGORY_ID, TRANSACTIONS.CATEGORY_ID)
        val total = DSL.sum(effectiveAmount).`as`("total")

        return dsl.select(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.COLOR, CATEGORIES.ICON, total)
            .from(TRANSACTIONS)
            .leftJoin(TRANSACTION_SPLITS).on(TRANSACTION_SPLITS.TRANSACTION_ID.eq(TRANSACTIONS.ID))
            .join(CATEGORIES).on(CATEGORIES.ID.eq(effectiveCategoryId))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                baseCondition(accountId, authenticatedUser)
                    .and(expenseInPeriod(from, to))
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
        val today = LocalDate.now()
        val firstMonth = YearMonth.from(today).minusMonths((months - 1).toLong())
        val start = firstMonth.atDay(1)

        val monthExpr = DSL.field("to_char({0}, 'YYYY-MM')", String::class.java, TRANSACTIONS.TRANSACTION_DATE).`as`("ym")
        val incomeExpr = DSL.sum(
            DSL.case_().`when`(TRANSACTIONS.TYPE.eq(CategoryType.INCOME.value), TRANSACTIONS.AMOUNT)
                .otherwise(BigDecimal.ZERO)
        ).`as`("income")
        val expenseExpr = DSL.sum(
            DSL.case_().`when`(TRANSACTIONS.TYPE.eq(CategoryType.EXPENSE.value), TRANSACTIONS.AMOUNT)
                .otherwise(BigDecimal.ZERO)
        ).`as`("expense")

        val rows = dsl.select(monthExpr, incomeExpr, expenseExpr)
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                baseCondition(accountId, authenticatedUser)
                    .and(TRANSACTIONS.TRANSACTION_DATE.ge(start))
                    .and(TRANSACTIONS.TRANSFER_GROUP_ID.isNull)
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

    override fun aggregateByDay(
        accountId: UUID, authenticatedUser: UserDTO, days: Int
    ): List<DailyAggregateDTO> {
        require(days in 1..731) { "days must be between 1 and 731" }
        val start = LocalDate.now().minusDays((days - 1).toLong())

        val dayExpr = DSL.field("to_char({0}, 'YYYY-MM-DD')", String::class.java, TRANSACTIONS.TRANSACTION_DATE).`as`("day")
        val incomeExpr = DSL.sum(
            DSL.case_().`when`(TRANSACTIONS.TYPE.eq(CategoryType.INCOME.value), TRANSACTIONS.AMOUNT)
                .otherwise(BigDecimal.ZERO)
        ).`as`("income")
        val expenseExpr = DSL.sum(
            DSL.case_().`when`(TRANSACTIONS.TYPE.eq(CategoryType.EXPENSE.value), TRANSACTIONS.AMOUNT)
                .otherwise(BigDecimal.ZERO)
        ).`as`("expense")

        return dsl.select(dayExpr, incomeExpr, expenseExpr)
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(
                baseCondition(accountId, authenticatedUser)
                    .and(TRANSACTIONS.TRANSACTION_DATE.ge(start))
                    .and(TRANSACTIONS.TRANSFER_GROUP_ID.isNull)
            )
            .groupBy(dayExpr)
            .orderBy(dayExpr.asc())
            .fetch { record ->
                DailyAggregateDTO(
                    date = record[dayExpr]!!,
                    income = record[incomeExpr] ?: BigDecimal.ZERO,
                    expense = record[expenseExpr] ?: BigDecimal.ZERO,
                )
            }
    }

    override fun importBatch(
        accountId: UUID,
        rows: List<ImportTransactionRow>,
        conversions: List<ConversionResult>,
        hashes: List<String>,
        authenticatedUser: UserDTO,
        providerConnectionId: UUID?,
    ): BatchImportOutcome {
        require(rows.size == hashes.size) { "rows and hashes must have equal length" }
        require(rows.size == conversions.size) { "rows and conversions must have equal length" }
        if (rows.isEmpty()) return BatchImportOutcome(0, BigDecimal.ZERO)
        require(rows.all { it.type != null }) { "Every import row must have a type" }

        val ownsAccount = dsl.selectOne().from(ACCOUNTS)
            .where(ACCOUNTS.ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
            .fetchOne() != null
        if (!ownsAccount) throw LocalizedException.NotFound("error.account.notFound")

        val now = OffsetDateTime.now()
        val insertStep = dsl.insertInto(
            TRANSACTIONS,
            TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.CATEGORY_ID, TRANSACTIONS.AMOUNT,
            TRANSACTIONS.DESCRIPTION, TRANSACTIONS.TRANSACTION_DATE, TRANSACTIONS.TYPE,
            TRANSACTIONS.ORIGINAL_AMOUNT, TRANSACTIONS.ORIGINAL_CURRENCY,
            TRANSACTIONS.EXCHANGE_RATE, TRANSACTIONS.RATE_DATE,
            TRANSACTIONS.IMPORT_HASH, TRANSACTIONS.PROVIDER_CONNECTION_ID,
            TRANSACTIONS.CREATED_AT, TRANSACTIONS.MODIFIED_AT,
        )
        rows.forEachIndexed { i, row ->
            val conversion = conversions[i]
            val fx = FxColumns.from(conversion)
            insertStep.values(
                accountId, row.categoryId, conversion.convertedAmount,
                row.description, row.transactionDate, row.type!!.value,
                fx.originalAmount, fx.originalCurrency, fx.rate, fx.rateDate,
                hashes[i], providerConnectionId,
                now, now,
            )
        }

        val inserted = insertStep
            .onConflict(TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.IMPORT_HASH)
            .where(TRANSACTIONS.IMPORT_HASH.isNotNull)
            .doNothing()
            .returning(TRANSACTIONS.TYPE, TRANSACTIONS.AMOUNT)
            .fetch()

        if (inserted.isEmpty()) return BatchImportOutcome(0, BigDecimal.ZERO)

        val net = inserted.sumOf { rec ->
            val type = CategoryType.fromValue(rec[TRANSACTIONS.TYPE]!!)
            val amount = rec[TRANSACTIONS.AMOUNT]!!
            if (type == CategoryType.INCOME) amount else amount.negate()
        }

        return BatchImportOutcome(insertedCount = inserted.size, netBalanceAdjustment = net)
    }

    override fun fetchForRuleEvaluation(authenticatedUser: UserDTO): List<RuleCandidateTransaction> =
        dsl.select(
            TRANSACTIONS.ID, TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.DESCRIPTION,
            TRANSACTIONS.AMOUNT, TRANSACTIONS.TYPE, TRANSACTIONS.CATEGORY_ID, CATEGORIES.IS_MANAGED,
        )
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .where(
                ACCOUNTS.USER_ID.eq(authenticatedUser.id)
                    .and(TRANSACTIONS.TRANSFER_GROUP_ID.isNull)
                    .and(
                        DSL.notExists(
                            dsl.selectOne().from(TRANSACTION_SPLITS)
                                .where(TRANSACTION_SPLITS.TRANSACTION_ID.eq(TRANSACTIONS.ID))
                        )
                    )
            )
            .fetch { r ->
                RuleCandidateTransaction(
                    id = r[TRANSACTIONS.ID]!!,
                    accountId = r[TRANSACTIONS.ACCOUNT_ID]!!,
                    description = r[TRANSACTIONS.DESCRIPTION] ?: "",
                    amount = r[TRANSACTIONS.AMOUNT]!!,
                    type = CategoryType.fromValue(r[TRANSACTIONS.TYPE]!!),
                    categoryId = r[TRANSACTIONS.CATEGORY_ID]!!,
                    isManagedCategory = r[CATEGORIES.IS_MANAGED] ?: false,
                )
            }

    override fun setCategoryForTransactions(authenticatedUser: UserDTO, ids: List<UUID>, categoryId: Long): Int {
        if (ids.isEmpty()) return 0
        return dsl.update(TRANSACTIONS)
            .set(TRANSACTIONS.CATEGORY_ID, categoryId)
            .set(TRANSACTIONS.MODIFIED_AT, OffsetDateTime.now())
            .where(
                TRANSACTIONS.ID.`in`(ids).and(
                    TRANSACTIONS.ACCOUNT_ID.`in`(
                        dsl.select(ACCOUNTS.ID).from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
                    )
                )
            )
            .execute()
    }

    override fun fetchSplitsByTransactionIds(
        authenticatedUser: UserDTO,
        transactionIds: List<UUID>,
    ): Map<UUID, List<TransactionSplitDTO>> {
        if (transactionIds.isEmpty()) return emptyMap()
        return dsl.select(
            TRANSACTION_SPLITS.ID, TRANSACTION_SPLITS.TRANSACTION_ID, TRANSACTION_SPLITS.AMOUNT, TRANSACTION_SPLITS.NOTE,
            CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.ICON, CATEGORIES.TYPE, CATEGORIES.COLOR, CATEGORIES.USER_ID,
        )
            .from(TRANSACTION_SPLITS)
            .join(TRANSACTIONS).on(TRANSACTIONS.ID.eq(TRANSACTION_SPLITS.TRANSACTION_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTION_SPLITS.CATEGORY_ID))
            .where(
                TRANSACTION_SPLITS.TRANSACTION_ID.`in`(transactionIds)
                    .and(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
            )
            .orderBy(TRANSACTION_SPLITS.CREATED_AT.asc(), TRANSACTION_SPLITS.ID.asc())
            .fetch { r ->
                r[TRANSACTION_SPLITS.TRANSACTION_ID]!! to TransactionSplitDTO(
                    id = r[TRANSACTION_SPLITS.ID],
                    amount = r[TRANSACTION_SPLITS.AMOUNT],
                    note = r[TRANSACTION_SPLITS.NOTE],
                    category = CategoryDTO(
                        id = r[CATEGORIES.ID],
                        name = r[CATEGORIES.NAME],
                        icon = r[CATEGORIES.ICON],
                        color = r[CATEGORIES.COLOR],
                        type = r[CATEGORIES.TYPE]?.let { CategoryType.fromValue(it) },
                        isSystem = r[CATEGORIES.USER_ID] == null,
                    ),
                )
            }
            .groupBy({ it.first }, { it.second })
    }

    override fun replaceSplits(
        transactionId: UUID,
        splits: List<TransactionSplitForm>,
        authenticatedUser: UserDTO,
    ) {
        val ownsTransaction = dsl.selectOne()
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .where(TRANSACTIONS.ID.eq(transactionId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
            .fetchOne() != null
        if (!ownsTransaction) throw LocalizedException.NotFound("error.transaction.notFound")

        dsl.deleteFrom(TRANSACTION_SPLITS).where(TRANSACTION_SPLITS.TRANSACTION_ID.eq(transactionId)).execute()
        if (splits.isEmpty()) return

        val now = OffsetDateTime.now()
        var step = dsl.insertInto(
            TRANSACTION_SPLITS,
            TRANSACTION_SPLITS.TRANSACTION_ID, TRANSACTION_SPLITS.CATEGORY_ID,
            TRANSACTION_SPLITS.AMOUNT, TRANSACTION_SPLITS.NOTE, TRANSACTION_SPLITS.CREATED_AT,
        )
        splits.forEach { step = step.values(transactionId, it.categoryId, it.amount, it.note, now) }
        step.execute()
    }

    override fun deleteSplitsForTransactions(transactionIds: List<UUID>, authenticatedUser: UserDTO): Int {
        if (transactionIds.isEmpty()) return 0
        return dsl.deleteFrom(TRANSACTION_SPLITS)
            .where(
                TRANSACTION_SPLITS.TRANSACTION_ID.`in`(transactionIds)
                    .and(
                        TRANSACTION_SPLITS.TRANSACTION_ID.`in`(
                            dsl.select(TRANSACTIONS.ID).from(TRANSACTIONS)
                                .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
                                .where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
                        )
                    )
            )
            .execute()
    }

    private fun baseCondition(accountId: UUID, authenticatedUser: UserDTO) =
        TRANSACTIONS.ACCOUNT_ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
}
