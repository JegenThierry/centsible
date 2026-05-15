package beer.thierry.budgetplanner.jooq.repository

import beer.thierry.budgetplanner.api.model.category.CategoryDTO
import beer.thierry.budgetplanner.api.model.category.CategoryType
import beer.thierry.budgetplanner.api.model.transaction.ImportTransactionRow
import beer.thierry.budgetplanner.api.model.transaction.TransactionDTO
import beer.thierry.budgetplanner.api.model.transaction.TransactionForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.BatchImportOutcome
import beer.thierry.budgetplanner.api.repository.ITransactionRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Repository
class TransactionRepository(private val dsl: DSLContext) : ITransactionRepository {
    override fun fetchTransactions(
        accountId: UUID, authenticatedUser: UserDTO, page: Int, pageSize: Int
    ): List<TransactionDTO> {
        require(page >= 1) { "page must be >= 1" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
        val offset = (page - 1).toLong() * pageSize

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
            .where(baseCondition(accountId, authenticatedUser))
            .orderBy(TRANSACTIONS.TRANSACTION_DATE.desc(), TRANSACTIONS.ID.desc()).limit(pageSize).offset(offset)
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
