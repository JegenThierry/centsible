package beer.thierry.budgetplannerrest.repository.transactions

import beer.thierry.budgetplannerrest.model.category.CategoryDTO
import beer.thierry.budgetplannerrest.model.category.CategoryType
import beer.thierry.budgetplannerrest.model.transaction.TransactionDTO
import beer.thierry.budgetplannerrest.model.transaction.TransactionForm
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

@Repository
class TransactionRepository(private val dsl: DSLContext) : ITransactionRepository {
    private val transactionSelect = dsl.select(
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
    )

    override fun fetchTransactions(
        accountId: UUID, authenticatedUser: UserDTO, page: Int, pageSize: Int
    ): List<TransactionDTO> {
        require(page >= 1) { "page must be >= 1" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
        val offset = (page - 1).toLong() * pageSize

        return transactionSelect.from(TRANSACTIONS).join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID)).where(baseCondition(accountId, authenticatedUser))
            .orderBy(TRANSACTIONS.TRANSACTION_DATE.desc(), TRANSACTIONS.ID.desc()).limit(pageSize).offset(offset)
            .fetch { mapToTransactionDTO(it) }
    }

    override fun fetchTransactionById(
        transactionId: UUID, authenticatedUser: UserDTO
    ): TransactionDTO {
        return transactionSelect.from(TRANSACTIONS).join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
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
        val record = dsl.insertInto(TRANSACTIONS).set(TRANSACTIONS.ACCOUNT_ID, accountId)
            .set(TRANSACTIONS.CATEGORY_ID, transactionForm.categoryId).set(TRANSACTIONS.AMOUNT, transactionForm.amount)
            .set(TRANSACTIONS.DESCRIPTION, transactionForm.description)
            .set(TRANSACTIONS.TRANSACTION_DATE, transactionForm.transactionDate)
            .set(TRANSACTIONS.CREATED_AT, OffsetDateTime.now()).set(TRANSACTIONS.MODIFIED_AT, OffsetDateTime.now())
            .returning().fetchOne() ?: throw IllegalStateException("Failed to create transaction")

        return fetchTransactionById(record[TRANSACTIONS.ID]!!, authenticatedUser)
    }

    override fun updateTransaction(
        transactionId: UUID, accountId: UUID, transactionForm: TransactionForm, authenticatedUser: UserDTO
    ): TransactionDTO {
        dsl.update(TRANSACTIONS).set(TRANSACTIONS.CATEGORY_ID, transactionForm.categoryId)
            .set(TRANSACTIONS.AMOUNT, transactionForm.amount)
            .set(TRANSACTIONS.DESCRIPTION, transactionForm.description)
            .set(TRANSACTIONS.TRANSACTION_DATE, transactionForm.transactionDate)
            .set(TRANSACTIONS.MODIFIED_AT, OffsetDateTime.now()).where(
                TRANSACTIONS.ID.eq(transactionId).and(TRANSACTIONS.ACCOUNT_ID.eq(accountId))
            ).execute()

        return fetchTransactionById(transactionId, authenticatedUser)
    }

    override fun deleteTransaction(transactionId: UUID, authenticatedUser: UserDTO): TransactionDTO {
        val transaction = fetchTransactionById(transactionId, authenticatedUser)

        dsl.deleteFrom(TRANSACTIONS).where(TRANSACTIONS.ID.eq(transactionId)).execute()

        return transaction
    }

    private fun baseCondition(accountId: UUID, authenticatedUser: UserDTO) =
        TRANSACTIONS.ACCOUNT_ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
}
