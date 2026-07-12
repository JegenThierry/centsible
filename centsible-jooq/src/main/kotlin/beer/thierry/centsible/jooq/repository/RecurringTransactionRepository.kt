package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.recurring.RecurringTransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.RECURRING_TRANSACTIONS
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@Repository
class RecurringTransactionRepository(private val dsl: DSLContext) : IRecurringTransactionRepository {

    private data class TransferAwareFields(
        val isTransfer: Boolean,
        val originalAmount: BigDecimal?,
        val originalCurrency: String?,
        val categoryId: Long?,
        val type: String?,
        val destinationAccountId: UUID?,
    )

    private fun resolveTransferAwareFields(form: RecurringTransactionForm): TransferAwareFields {
        val isTransfer = form.isTransfer || form.destinationAccountId != null
        val transferCategoryId = if (isTransfer) {
            dsl.findSystemCategoryId(ManagedCategoryNames.TRANSFER)
                ?: throw IllegalStateException("System 'Transfer' category not found. Migration may not have run.")
        } else {
            null
        }
        return TransferAwareFields(
            isTransfer = isTransfer,
            originalAmount = if (isTransfer) null else form.currency?.let { form.amount },
            originalCurrency = if (isTransfer) null else form.currency?.name,
            categoryId = if (isTransfer) transferCategoryId else form.categoryId,
            type = if (isTransfer) null else form.type?.value,
            destinationAccountId = if (isTransfer) form.destinationAccountId else null,
        )
    }

    override fun fetchAll(authenticatedUser: UserDTO, accountId: UUID?): List<RecurringTransactionDTO> {
        val ownership = ACCOUNTS.USER_ID.eq(authenticatedUser.id)
        val filter: Condition = if (accountId != null) ownership.and(ACCOUNTS.ID.eq(accountId)) else ownership

        return baseSelect()
            .where(filter.and(categoryOwnedBy(authenticatedUser)))
            .orderBy(RECURRING_TRANSACTIONS.ACTIVE.desc(), RECURRING_TRANSACTIONS.NEXT_RUN_AT.asc())
            .fetch { mapToDTO(it) }
    }

    override fun fetchById(id: UUID, authenticatedUser: UserDTO): RecurringTransactionDTO {
        return baseSelect()
            .where(
                RECURRING_TRANSACTIONS.ID.eq(id)
                    .and(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
                    .and(categoryOwnedBy(authenticatedUser))
            )
            .fetchSingle { mapToDTO(it) }
    }

    private fun categoryOwnedBy(authenticatedUser: UserDTO): Condition =
        CATEGORIES.USER_ID.eq(authenticatedUser.id).or(CATEGORIES.USER_ID.isNull)

    override fun create(
        accountId: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO
    ): RecurringTransactionDTO {
        val fields = resolveTransferAwareFields(form)
        val now = OffsetDateTime.now()
        val record = dsl.insertInto(
            RECURRING_TRANSACTIONS,
            RECURRING_TRANSACTIONS.ACCOUNT_ID,
            RECURRING_TRANSACTIONS.CATEGORY_ID,
            RECURRING_TRANSACTIONS.AMOUNT,
            RECURRING_TRANSACTIONS.DESCRIPTION,
            RECURRING_TRANSACTIONS.FREQUENCY,
            RECURRING_TRANSACTIONS.START_DATE,
            RECURRING_TRANSACTIONS.END_DATE,
            RECURRING_TRANSACTIONS.NEXT_RUN_AT,
            RECURRING_TRANSACTIONS.ACTIVE,
            RECURRING_TRANSACTIONS.ORIGINAL_AMOUNT,
            RECURRING_TRANSACTIONS.ORIGINAL_CURRENCY,
            RECURRING_TRANSACTIONS.IS_TRANSFER,
            RECURRING_TRANSACTIONS.DESTINATION_ACCOUNT_ID,
            RECURRING_TRANSACTIONS.TYPE,
            RECURRING_TRANSACTIONS.CREATED_AT,
            RECURRING_TRANSACTIONS.MODIFIED_AT,
        )
            .select(
                dsl.select(
                    DSL.value(accountId),
                    DSL.value(fields.categoryId, RECURRING_TRANSACTIONS.CATEGORY_ID),
                    DSL.value(form.amount),
                    DSL.value(form.description),
                    DSL.value(form.frequency.name),
                    DSL.value(form.startDate),
                    DSL.value(form.endDate),
                    DSL.value(form.startDate),
                    DSL.value(form.active),
                    DSL.value(fields.originalAmount, RECURRING_TRANSACTIONS.ORIGINAL_AMOUNT),
                    DSL.value(fields.originalCurrency, RECURRING_TRANSACTIONS.ORIGINAL_CURRENCY),
                    DSL.value(fields.isTransfer),
                    DSL.value(fields.destinationAccountId, RECURRING_TRANSACTIONS.DESTINATION_ACCOUNT_ID),
                    DSL.value(fields.type, RECURRING_TRANSACTIONS.TYPE),
                    DSL.value(now),
                    DSL.value(now),
                ).whereExists(
                    dsl.selectOne().from(ACCOUNTS)
                        .where(ACCOUNTS.ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
                )
            )
            .returning(RECURRING_TRANSACTIONS.ID)
            .fetchOne()
            ?: throw LocalizedException.NotFound("error.account.notFound")

        return fetchById(record[RECURRING_TRANSACTIONS.ID]!!, authenticatedUser)
    }

    override fun update(
        id: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO
    ): RecurringTransactionDTO {
        val fields = resolveTransferAwareFields(form)
        val updated = dsl.update(RECURRING_TRANSACTIONS)
            .set(RECURRING_TRANSACTIONS.CATEGORY_ID, fields.categoryId)
            .set(RECURRING_TRANSACTIONS.AMOUNT, form.amount)
            .set(RECURRING_TRANSACTIONS.DESCRIPTION, form.description)
            .set(RECURRING_TRANSACTIONS.FREQUENCY, form.frequency.name)
            .set(RECURRING_TRANSACTIONS.START_DATE, form.startDate)
            .set(RECURRING_TRANSACTIONS.END_DATE, form.endDate)
            .set(RECURRING_TRANSACTIONS.ACTIVE, form.active)
            .set(RECURRING_TRANSACTIONS.ORIGINAL_AMOUNT, fields.originalAmount)
            .set(RECURRING_TRANSACTIONS.ORIGINAL_CURRENCY, fields.originalCurrency)
            .set(RECURRING_TRANSACTIONS.IS_TRANSFER, fields.isTransfer)
            .set(RECURRING_TRANSACTIONS.DESTINATION_ACCOUNT_ID, fields.destinationAccountId)
            .set(RECURRING_TRANSACTIONS.TYPE, fields.type)
            .set(RECURRING_TRANSACTIONS.MODIFIED_AT, OffsetDateTime.now())
            .where(
                RECURRING_TRANSACTIONS.ID.eq(id).and(
                    RECURRING_TRANSACTIONS.ACCOUNT_ID.`in`(
                        dsl.select(ACCOUNTS.ID).from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
                    )
                )
            )
            .execute()

        if (updated == 0) throw IllegalArgumentException("Recurring transaction not found or not owned by user")
        return fetchById(id, authenticatedUser)
    }

    override fun delete(id: UUID, authenticatedUser: UserDTO) {
        val deleted = dsl.deleteFrom(RECURRING_TRANSACTIONS)
            .where(
                RECURRING_TRANSACTIONS.ID.eq(id).and(
                    RECURRING_TRANSACTIONS.ACCOUNT_ID.`in`(
                        dsl.select(ACCOUNTS.ID).from(ACCOUNTS).where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
                    )
                )
            )
            .execute()
        if (deleted == 0) throw IllegalArgumentException("Recurring transaction not found or not owned by user")
    }

    override fun fetchDueRules(today: LocalDate): List<RecurringTransactionDTO> {
        return baseSelect()
            .where(
                RECURRING_TRANSACTIONS.ACTIVE.eq(true)
                    .and(RECURRING_TRANSACTIONS.NEXT_RUN_AT.le(today))
                    .and(
                        RECURRING_TRANSACTIONS.END_DATE.isNull
                            .or(RECURRING_TRANSACTIONS.NEXT_RUN_AT.le(RECURRING_TRANSACTIONS.END_DATE))
                    )
            )
            .orderBy(RECURRING_TRANSACTIONS.NEXT_RUN_AT.asc())
            .fetch { mapToDTO(it) }
    }

    @Transactional
    override fun materializeOnce(rule: RecurringTransactionDTO, conversion: ConversionResult): LocalDate? {
        val ruleId = rule.id ?: throw IllegalStateException("Rule id missing")
        val accountId = rule.accountId ?: throw IllegalStateException("Rule account missing")
        val occurrenceDate = rule.nextRunAt ?: throw IllegalStateException("Rule next_run_at missing")
        val frequency = rule.frequency ?: throw IllegalStateException("Rule frequency missing")
        val now = OffsetDateTime.now()

        val newNext = frequency.advance(occurrenceDate)
        val endDate = rule.endDate
        val deactivate = endDate != null && newNext > endDate

        val advanced = dsl.update(RECURRING_TRANSACTIONS)
            .set(RECURRING_TRANSACTIONS.NEXT_RUN_AT, newNext)
            .set(RECURRING_TRANSACTIONS.ACTIVE, !deactivate)
            .set(RECURRING_TRANSACTIONS.MODIFIED_AT, now)
            .where(
                RECURRING_TRANSACTIONS.ID.eq(ruleId)
                    .and(RECURRING_TRANSACTIONS.NEXT_RUN_AT.eq(occurrenceDate))
                    .and(RECURRING_TRANSACTIONS.ACTIVE.eq(true))
            )
            .execute()

        if (advanced == 0) return null

        if (rule.isTransfer) {
            val destinationAccountId = rule.destinationAccountId
                ?: throw IllegalStateException("Transfer rule missing destination account")
            val outCategoryId = dsl.findManagedCategoryId(ManagedCategoryNames.TRANSFER_OUT)
                ?: throw IllegalStateException("System 'Transfer out' category not found. Migration may not have run.")
            val inCategoryId = dsl.findManagedCategoryId(ManagedCategoryNames.TRANSFER_IN)
                ?: throw IllegalStateException("System 'Transfer in' category not found. Migration may not have run.")

            val sourceAmount = rule.amount ?: conversion.originalAmount
            val destAmount = conversion.convertedAmount
            val destFx = FxColumns.from(conversion)
            val groupId = UUID.randomUUID()

            dsl.insertInto(
                TRANSACTIONS,
                TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.CATEGORY_ID, TRANSACTIONS.AMOUNT,
                TRANSACTIONS.DESCRIPTION, TRANSACTIONS.TRANSACTION_DATE, TRANSACTIONS.TYPE,
                TRANSACTIONS.RECURRING_TRANSACTION_ID, TRANSACTIONS.TRANSFER_GROUP_ID,
                TRANSACTIONS.CREATED_AT, TRANSACTIONS.MODIFIED_AT,
            ).values(
                accountId, outCategoryId, sourceAmount,
                rule.description, occurrenceDate, CategoryType.EXPENSE.value,
                ruleId, groupId,
                now, now,
            ).execute()

            dsl.insertInto(
                TRANSACTIONS,
                TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.CATEGORY_ID, TRANSACTIONS.AMOUNT,
                TRANSACTIONS.DESCRIPTION, TRANSACTIONS.TRANSACTION_DATE, TRANSACTIONS.TYPE,
                TRANSACTIONS.ORIGINAL_AMOUNT, TRANSACTIONS.ORIGINAL_CURRENCY,
                TRANSACTIONS.EXCHANGE_RATE, TRANSACTIONS.RATE_DATE,
                TRANSACTIONS.RECURRING_TRANSACTION_ID, TRANSACTIONS.TRANSFER_GROUP_ID,
                TRANSACTIONS.CREATED_AT, TRANSACTIONS.MODIFIED_AT,
            ).values(
                destinationAccountId, inCategoryId, destAmount,
                rule.description, occurrenceDate, CategoryType.INCOME.value,
                destFx.originalAmount, destFx.originalCurrency, destFx.rate, destFx.rateDate,
                ruleId, groupId,
                now, now,
            ).execute()

            dsl.update(ACCOUNTS)
                .set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.minus(sourceAmount))
                .set(ACCOUNTS.MODIFIED_AT, now)
                .where(ACCOUNTS.ID.eq(accountId))
                .execute()
            dsl.update(ACCOUNTS)
                .set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.plus(destAmount))
                .set(ACCOUNTS.MODIFIED_AT, now)
                .where(ACCOUNTS.ID.eq(destinationAccountId))
                .execute()
        } else {
            val type = rule.type ?: rule.category.type
                ?: throw IllegalStateException("Rule direction missing")
            val converted = conversion.convertedAmount
            val fx = FxColumns.from(conversion)
            dsl.insertInto(
                TRANSACTIONS,
                TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.CATEGORY_ID, TRANSACTIONS.AMOUNT,
                TRANSACTIONS.DESCRIPTION, TRANSACTIONS.TRANSACTION_DATE, TRANSACTIONS.TYPE,
                TRANSACTIONS.ORIGINAL_AMOUNT, TRANSACTIONS.ORIGINAL_CURRENCY,
                TRANSACTIONS.EXCHANGE_RATE, TRANSACTIONS.RATE_DATE,
                TRANSACTIONS.RECURRING_TRANSACTION_ID,
                TRANSACTIONS.CREATED_AT, TRANSACTIONS.MODIFIED_AT,
            ).values(
                accountId, rule.category.id, converted,
                rule.description, occurrenceDate, type.value,
                fx.originalAmount, fx.originalCurrency, fx.rate, fx.rateDate,
                ruleId,
                now, now,
            ).execute()

            val adjustment = if (type == CategoryType.INCOME) converted else converted.negate()
            dsl.update(ACCOUNTS)
                .set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.plus(adjustment))
                .set(ACCOUNTS.MODIFIED_AT, now)
                .where(ACCOUNTS.ID.eq(accountId))
                .execute()
        }

        return if (deactivate) null else newNext
    }

    private fun baseSelect() = dsl.select(
        RECURRING_TRANSACTIONS.ID,
        RECURRING_TRANSACTIONS.ACCOUNT_ID,
        RECURRING_TRANSACTIONS.AMOUNT,
        RECURRING_TRANSACTIONS.DESCRIPTION,
        RECURRING_TRANSACTIONS.FREQUENCY,
        RECURRING_TRANSACTIONS.START_DATE,
        RECURRING_TRANSACTIONS.END_DATE,
        RECURRING_TRANSACTIONS.NEXT_RUN_AT,
        RECURRING_TRANSACTIONS.ACTIVE,
        RECURRING_TRANSACTIONS.ORIGINAL_AMOUNT,
        RECURRING_TRANSACTIONS.ORIGINAL_CURRENCY,
        RECURRING_TRANSACTIONS.IS_TRANSFER,
        RECURRING_TRANSACTIONS.DESTINATION_ACCOUNT_ID,
        RECURRING_TRANSACTIONS.TYPE,
        RECURRING_TRANSACTIONS.CREATED_AT,
        RECURRING_TRANSACTIONS.MODIFIED_AT,
        CATEGORIES.ID,
        CATEGORIES.NAME,
        CATEGORIES.ICON,
        CATEGORIES.TYPE,
        CATEGORIES.COLOR,
    )
        .from(RECURRING_TRANSACTIONS)
        .leftJoin(CATEGORIES).on(CATEGORIES.ID.eq(RECURRING_TRANSACTIONS.CATEGORY_ID))
        .join(ACCOUNTS).on(ACCOUNTS.ID.eq(RECURRING_TRANSACTIONS.ACCOUNT_ID))

    private fun mapToDTO(record: org.jooq.Record): RecurringTransactionDTO {
        val category = record[CATEGORIES.ID]?.let { categoryId ->
            CategoryDTO(
                id = categoryId,
                name = record[CATEGORIES.NAME]!!,
                icon = record[CATEGORIES.ICON]!!,
                type = record[CATEGORIES.TYPE]?.let { CategoryType.fromValue(it) },
                color = record[CATEGORIES.COLOR],
            )
        } ?: CategoryDTO()

        return RecurringTransactionDTO(
            id = record[RECURRING_TRANSACTIONS.ID]!!,
            accountId = record[RECURRING_TRANSACTIONS.ACCOUNT_ID]!!,
            category = category,
            amount = record[RECURRING_TRANSACTIONS.AMOUNT]!!,
            description = record[RECURRING_TRANSACTIONS.DESCRIPTION]!!,
            frequency = Frequency.fromValue(record[RECURRING_TRANSACTIONS.FREQUENCY]!!),
            startDate = record[RECURRING_TRANSACTIONS.START_DATE]!!,
            endDate = record[RECURRING_TRANSACTIONS.END_DATE],
            nextRunAt = record[RECURRING_TRANSACTIONS.NEXT_RUN_AT]!!,
            active = record[RECURRING_TRANSACTIONS.ACTIVE]!!,
            createdAt = record[RECURRING_TRANSACTIONS.CREATED_AT]!!,
            updatedAt = record[RECURRING_TRANSACTIONS.MODIFIED_AT]!!,
            originalAmount = record[RECURRING_TRANSACTIONS.ORIGINAL_AMOUNT],
            originalCurrency = record[RECURRING_TRANSACTIONS.ORIGINAL_CURRENCY]?.let { Currency.valueOf(it) },
            type = record[RECURRING_TRANSACTIONS.TYPE]?.let { CategoryType.fromValue(it) },
            isTransfer = record[RECURRING_TRANSACTIONS.IS_TRANSFER] ?: false,
            destinationAccountId = record[RECURRING_TRANSACTIONS.DESTINATION_ACCOUNT_ID],
        )
    }
}
