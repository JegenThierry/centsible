package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import org.jooq.Field
import org.jooq.Record

internal object TransactionRecordMapper {

    val columns: Array<Field<*>> = arrayOf(
        TRANSACTIONS.ID,
        TRANSACTIONS.AMOUNT,
        TRANSACTIONS.DESCRIPTION,
        TRANSACTIONS.TRANSACTION_DATE,
        TRANSACTIONS.TYPE,
        TRANSACTIONS.CREATED_AT,
        TRANSACTIONS.MODIFIED_AT,
        TRANSACTIONS.ORIGINAL_AMOUNT,
        TRANSACTIONS.ORIGINAL_CURRENCY,
        TRANSACTIONS.EXCHANGE_RATE,
        TRANSACTIONS.RATE_DATE,
        TRANSACTIONS.TRANSFER_GROUP_ID,
        CATEGORIES.ID,
        CATEGORIES.NAME,
        CATEGORIES.ICON,
        CATEGORIES.TYPE,
        CATEGORIES.COLOR,
        CATEGORIES.USER_ID,
    )

    fun mapTransaction(record: Record): TransactionDTO = TransactionDTO(
        id = record[TRANSACTIONS.ID]!!,
        category = mapCategory(record),
        type = CategoryType.fromValue(record[TRANSACTIONS.TYPE]!!),
        amount = record[TRANSACTIONS.AMOUNT]!!,
        description = record[TRANSACTIONS.DESCRIPTION],
        transactionDate = record[TRANSACTIONS.TRANSACTION_DATE]!!,
        createdAt = record[TRANSACTIONS.CREATED_AT]!!,
        updatedAt = record[TRANSACTIONS.MODIFIED_AT]!!,
        originalAmount = record[TRANSACTIONS.ORIGINAL_AMOUNT],
        originalCurrency = record[TRANSACTIONS.ORIGINAL_CURRENCY]?.let { Currency.valueOf(it) },
        exchangeRate = record[TRANSACTIONS.EXCHANGE_RATE],
        rateDate = record[TRANSACTIONS.RATE_DATE],
        transferGroupId = record[TRANSACTIONS.TRANSFER_GROUP_ID],
    )

    fun mapTransactionOrNull(record: Record): TransactionDTO? =
        if (record[TRANSACTIONS.ID] != null) mapTransaction(record) else null

    private fun mapCategory(record: Record) = CategoryDTO(
        id = record[CATEGORIES.ID],
        name = record[CATEGORIES.NAME],
        icon = record[CATEGORIES.ICON],
        color = record[CATEGORIES.COLOR],
        type = record[CATEGORIES.TYPE]?.let { CategoryType.fromValue(it) },
        isSystem = record[CATEGORIES.USER_ID] == null,
    )
}
