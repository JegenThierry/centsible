package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.export.ExportAccountRow
import beer.thierry.centsible.api.model.export.ExportContactSummaryRow
import beer.thierry.centsible.api.model.export.ExportLoanRow
import beer.thierry.centsible.api.model.export.ExportTransactionRow
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.CONTACTS
import beer.thierry.jooq.generated.tables.references.CONTACT_BALANCES
import beer.thierry.jooq.generated.tables.references.LOANS
import beer.thierry.jooq.generated.tables.references.LOAN_REPAYMENTS
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import beer.thierry.jooq.generated.tables.references.USERS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Repository
class ExportDataRepository(private val dsl: DSLContext) : IExportDataRepository {

    override fun fetchUserById(userId: UUID): UserDTO? =
        dsl.select(
            USERS.ID,
            USERS.USERNAME,
            USERS.EMAIL,
            USERS.FIRST_NAME,
            USERS.LAST_NAME,
            USERS.FIRST_NAME.concat(" ").concat(USERS.LAST_NAME).`as`("name"),
            USERS.PROFILE_PICTURE,
        )
            .from(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOneInto(UserDTO::class.java)

    override fun fetchTransactionsForExport(
        userId: UUID,
        accountIds: List<UUID>,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        categoryIds: List<Long>,
    ): List<ExportTransactionRow> {
        val conditions = mutableListOf<Condition>(ACCOUNTS.USER_ID.eq(userId))
        if (accountIds.isNotEmpty()) conditions += TRANSACTIONS.ACCOUNT_ID.`in`(accountIds)
        if (fromDate != null) conditions += TRANSACTIONS.TRANSACTION_DATE.ge(fromDate)
        if (toDate != null) conditions += TRANSACTIONS.TRANSACTION_DATE.le(toDate)
        if (categoryIds.isNotEmpty()) conditions += TRANSACTIONS.CATEGORY_ID.`in`(categoryIds)

        return dsl.select(
            TRANSACTIONS.ID,
            TRANSACTIONS.ACCOUNT_ID,
            ACCOUNTS.NAME,
            ACCOUNTS.CURRENCY,
            TRANSACTIONS.TRANSACTION_DATE,
            TRANSACTIONS.AMOUNT,
            TRANSACTIONS.DESCRIPTION,
            TRANSACTIONS.TYPE,
            CATEGORIES.NAME,
            CATEGORIES.COLOR,
        )
            .from(TRANSACTIONS)
            .join(ACCOUNTS).on(ACCOUNTS.ID.eq(TRANSACTIONS.ACCOUNT_ID))
            .join(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .where(DSL.and(conditions))
            .orderBy(TRANSACTIONS.TRANSACTION_DATE.asc(), TRANSACTIONS.ID.asc())
            .fetch { r ->
                ExportTransactionRow(
                    id = r[TRANSACTIONS.ID]!!,
                    accountId = r[TRANSACTIONS.ACCOUNT_ID]!!,
                    accountName = r[ACCOUNTS.NAME]!!,
                    currency = r[ACCOUNTS.CURRENCY]!!,
                    transactionDate = r[TRANSACTIONS.TRANSACTION_DATE]!!,
                    amount = r[TRANSACTIONS.AMOUNT]!!,
                    description = r[TRANSACTIONS.DESCRIPTION],
                    categoryName = r[CATEGORIES.NAME]!!,
                    categoryType = CategoryType.fromValue(r[TRANSACTIONS.TYPE]!!),
                    categoryColor = r[CATEGORIES.COLOR]!!,
                )
            }
    }

    override fun fetchAccountsByIds(userId: UUID, accountIds: List<UUID>): List<ExportAccountRow> {
        val conditions = mutableListOf<Condition>(ACCOUNTS.USER_ID.eq(userId))
        if (accountIds.isNotEmpty()) conditions += ACCOUNTS.ID.`in`(accountIds)

        return dsl.select(
            ACCOUNTS.ID,
            ACCOUNTS.NAME,
            ACCOUNTS.CURRENCY,
            ACCOUNTS.INITIAL_BALANCE,
            ACCOUNTS.BALANCE,
            ACCOUNTS.CREATED_AT,
        )
            .from(ACCOUNTS)
            .where(DSL.and(conditions))
            .orderBy(ACCOUNTS.NAME.asc())
            .fetch { r ->
                ExportAccountRow(
                    id = r[ACCOUNTS.ID]!!,
                    name = r[ACCOUNTS.NAME]!!,
                    currency = r[ACCOUNTS.CURRENCY]!!,
                    initialBalance = r[ACCOUNTS.INITIAL_BALANCE]!!,
                    balance = r[ACCOUNTS.BALANCE]!!,
                    createdAt = r[ACCOUNTS.CREATED_AT]!!,
                )
            }
    }

    override fun fetchAllAccounts(userId: UUID, asOfDate: LocalDate?): List<ExportAccountRow> {
        if (asOfDate == null) return fetchAccountsByIds(userId, emptyList())

        val signedAmount = DSL.case_()
            .`when`(TRANSACTIONS.TYPE.eq("INCOME"), TRANSACTIONS.AMOUNT)
            .otherwise(TRANSACTIONS.AMOUNT.neg())

        val asOfBalance = ACCOUNTS.INITIAL_BALANCE.plus(
            DSL.coalesce(
                DSL.select(DSL.sum(signedAmount))
                    .from(TRANSACTIONS)
                    .where(
                        TRANSACTIONS.ACCOUNT_ID.eq(ACCOUNTS.ID)
                            .and(TRANSACTIONS.TRANSACTION_DATE.le(asOfDate))
                    ),
                BigDecimal.ZERO,
            )
        ).`as`("as_of_balance")

        return dsl.select(
            ACCOUNTS.ID,
            ACCOUNTS.NAME,
            ACCOUNTS.CURRENCY,
            ACCOUNTS.INITIAL_BALANCE,
            asOfBalance,
            ACCOUNTS.CREATED_AT,
        )
            .from(ACCOUNTS)
            .where(ACCOUNTS.USER_ID.eq(userId))
            .orderBy(ACCOUNTS.NAME.asc())
            .fetch { r ->
                ExportAccountRow(
                    id = r[ACCOUNTS.ID]!!,
                    name = r[ACCOUNTS.NAME]!!,
                    currency = r[ACCOUNTS.CURRENCY]!!,
                    initialBalance = r[ACCOUNTS.INITIAL_BALANCE]!!,
                    balance = r.get("as_of_balance", BigDecimal::class.java) ?: BigDecimal.ZERO,
                    createdAt = r[ACCOUNTS.CREATED_AT]!!,
                )
            }
    }

    override fun fetchLoansForContact(userId: UUID, contactId: UUID): List<ExportLoanRow> =
        fetchLoans(userId, includeSettled = true, contactId = contactId)

    override fun fetchAllLoans(userId: UUID, includeSettled: Boolean): List<ExportLoanRow> =
        fetchLoans(userId, includeSettled = includeSettled, contactId = null)

    private fun fetchLoans(
        userId: UUID,
        includeSettled: Boolean,
        contactId: UUID?,
    ): List<ExportLoanRow> {
        val repaidByLoan = DSL.select(
            LOAN_REPAYMENTS.LOAN_ID,
            DSL.sum(LOAN_REPAYMENTS.AMOUNT).`as`("repaid_total"),
        )
            .from(LOAN_REPAYMENTS)
            .groupBy(LOAN_REPAYMENTS.LOAN_ID)
            .asTable("rp")

        val repaidTotal = DSL.coalesce(repaidByLoan.field("repaid_total", BigDecimal::class.java), BigDecimal.ZERO)
        val outstanding = LOANS.OWED_AMOUNT.minus(repaidTotal)

        val contactName = CONTACTS.FIRST_NAME.concat(
            DSL.case_()
                .`when`(CONTACTS.LAST_NAME.isNull, DSL.value(""))
                .otherwise(DSL.value(" ").concat(CONTACTS.LAST_NAME))
        )

        val conditions = mutableListOf<Condition>(LOANS.USER_ID.eq(userId))
        if (contactId != null) conditions += LOANS.CONTACT_ID.eq(contactId)
        if (!includeSettled) conditions += outstanding.gt(BigDecimal.ZERO)

        return dsl.select(
            LOANS.ID,
            LOANS.CONTACT_ID,
            contactName.`as`("contact_name"),
            LOANS.LENT_AMOUNT,
            LOANS.OWED_AMOUNT,
            repaidTotal.`as`("repaid_total"),
            outstanding.`as`("outstanding"),
            LOANS.LOAN_DATE,
            LOANS.DUE_DATE,
            LOANS.DESCRIPTION,
            LOANS.CREATED_AT,
        )
            .from(LOANS)
            .join(CONTACTS).on(CONTACTS.ID.eq(LOANS.CONTACT_ID))
            .leftJoin(repaidByLoan).on(repaidByLoan.field("loan_id", UUID::class.java)!!.eq(LOANS.ID))
            .where(DSL.and(conditions))
            .orderBy(LOANS.LOAN_DATE.desc(), LOANS.CREATED_AT.desc())
            .fetch { r ->
                ExportLoanRow(
                    id = r[LOANS.ID]!!,
                    contactId = r[LOANS.CONTACT_ID]!!,
                    contactName = r.get("contact_name", String::class.java) ?: "",
                    lentAmount = r[LOANS.LENT_AMOUNT]!!,
                    owedAmount = r[LOANS.OWED_AMOUNT]!!,
                    totalRepaid = r.get("repaid_total", BigDecimal::class.java) ?: BigDecimal.ZERO,
                    outstanding = r.get("outstanding", BigDecimal::class.java) ?: BigDecimal.ZERO,
                    loanDate = r[LOANS.LOAN_DATE]!!,
                    dueDate = r[LOANS.DUE_DATE],
                    description = r[LOANS.DESCRIPTION],
                    createdAt = r[LOANS.CREATED_AT]!!,
                )
            }
    }

    override fun fetchContactSummary(userId: UUID, contactId: UUID): ExportContactSummaryRow? =
        contactSummaryQuery(userId)
            .and(CONTACT_BALANCES.CONTACT_ID.eq(contactId))
            .fetchOne(::mapContactSummary)

    override fun fetchAllContactSummaries(userId: UUID): List<ExportContactSummaryRow> =
        contactSummaryQuery(userId)
            .orderBy(CONTACTS.FIRST_NAME.asc(), CONTACTS.LAST_NAME.asc())
            .fetch(::mapContactSummary)

    private fun contactSummaryQuery(userId: UUID) =
        dsl.select(
            CONTACT_BALANCES.CONTACT_ID,
            CONTACTS.FIRST_NAME.concat(
                DSL.case_()
                    .`when`(CONTACTS.LAST_NAME.isNull, DSL.value(""))
                    .otherwise(DSL.value(" ").concat(CONTACTS.LAST_NAME))
            ).`as`("contact_name"),
            CONTACT_BALANCES.TOTAL_LENT,
            CONTACT_BALANCES.TOTAL_OWED,
            CONTACT_BALANCES.TOTAL_REPAID,
            CONTACT_BALANCES.OUTSTANDING,
            CONTACT_BALANCES.OPEN_LOAN_COUNT,
        )
            .from(CONTACT_BALANCES)
            .join(CONTACTS).on(CONTACTS.ID.eq(CONTACT_BALANCES.CONTACT_ID))
            .where(CONTACT_BALANCES.USER_ID.eq(userId))

    private fun mapContactSummary(r: org.jooq.Record): ExportContactSummaryRow =
        ExportContactSummaryRow(
            contactId = r[CONTACT_BALANCES.CONTACT_ID]!!,
            contactName = r.get("contact_name", String::class.java) ?: "",
            totalLent = r[CONTACT_BALANCES.TOTAL_LENT] ?: BigDecimal.ZERO,
            totalOwed = r[CONTACT_BALANCES.TOTAL_OWED] ?: BigDecimal.ZERO,
            totalRepaid = r[CONTACT_BALANCES.TOTAL_REPAID] ?: BigDecimal.ZERO,
            outstanding = r[CONTACT_BALANCES.OUTSTANDING] ?: BigDecimal.ZERO,
            openLoanCount = (r[CONTACT_BALANCES.OPEN_LOAN_COUNT] ?: 0L).toInt(),
        )
}
