package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.LoanUpdateForm
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.ILoansRepository
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.CONTACTS
import beer.thierry.jooq.generated.tables.references.LOANS
import beer.thierry.jooq.generated.tables.references.LOAN_REPAYMENTS
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class LoansRepository(
    private val dsl: DSLContext,
    private val budgetAccountsRepository: IBudgetAccountsRepository,
) : ILoansRepository {

    override fun fetchAllLoans(authenticatedUser: UserDTO): List<LoanDTO> =
        fetchLoansWhere(LOANS.USER_ID.eq(authenticatedUser.id))

    override fun fetchLoansByContact(authenticatedUser: UserDTO, contactId: UUID): List<LoanDTO> =
        fetchLoansWhere(LOANS.USER_ID.eq(authenticatedUser.id).and(LOANS.CONTACT_ID.eq(contactId)))

    override fun fetchLoanById(authenticatedUser: UserDTO, id: UUID): LoanDTO? =
        fetchLoansWhere(LOANS.USER_ID.eq(authenticatedUser.id).and(LOANS.ID.eq(id))).firstOrNull()

    override fun createLoan(
        authenticatedUser: UserDTO,
        contactId: UUID,
        form: LoanForm,
        currency: Currency,
        conversion: ConversionResult?,
    ): LoanDTO {
        val now = OffsetDateTime.now()
        val transactionId: UUID? = if (form.affectBalance) {
            val accountId = form.accountId
                ?: throw IllegalArgumentException("Account is required when the loan affects an account balance.")
            dsl.ensureAccountOwnedByUser(accountId, authenticatedUser.id)
            requireNotNull(conversion) { "Conversion is required for a balance-affecting loan." }

            val lendingCategoryId = dsl.findManagedCategoryId(ManagedCategoryNames.LENDING)
                ?: throw IllegalStateException("System Lending category not found. Migration may not have run.")

            val fx = FxColumns.from(conversion)
            val newTransactionId = dsl.insertInto(TRANSACTIONS)
                .set(TRANSACTIONS.ACCOUNT_ID, accountId)
                .set(TRANSACTIONS.CATEGORY_ID, lendingCategoryId)
                .set(TRANSACTIONS.AMOUNT, conversion.convertedAmount)
                .set(TRANSACTIONS.DESCRIPTION, form.description)
                .set(TRANSACTIONS.TRANSACTION_DATE, form.transactionDate)
                .set(TRANSACTIONS.TYPE, CategoryType.EXPENSE.value)
                .set(TRANSACTIONS.ORIGINAL_AMOUNT, fx.originalAmount)
                .set(TRANSACTIONS.ORIGINAL_CURRENCY, fx.originalCurrency)
                .set(TRANSACTIONS.EXCHANGE_RATE, fx.rate)
                .set(TRANSACTIONS.RATE_DATE, fx.rateDate)
                .set(TRANSACTIONS.CREATED_AT, now)
                .set(TRANSACTIONS.MODIFIED_AT, now)
                .returning(TRANSACTIONS.ID)
                .fetchOne()
                ?.get(TRANSACTIONS.ID)
                ?: throw IllegalStateException("Failed to create lending transaction")

            budgetAccountsRepository.updateBalance(accountId, conversion.convertedAmount.negate(), authenticatedUser)

            newTransactionId
        } else null

        val loanId = dsl.insertInto(LOANS)
            .set(LOANS.USER_ID, authenticatedUser.id)
            .set(LOANS.CONTACT_ID, contactId)
            .set(LOANS.TRANSACTION_ID, transactionId)
            .set(LOANS.LENT_AMOUNT, form.lentAmount)
            .set(LOANS.OWED_AMOUNT, form.owedAmount)
            .set(LOANS.CURRENCY, currency.name)
            .set(LOANS.INTEREST_RATE, form.interestRate)
            .set(LOANS.LOAN_DATE, form.transactionDate)
            .set(LOANS.DESCRIPTION, form.description)
            .set(LOANS.DUE_DATE, form.dueDate)
            .set(LOANS.NOTES, form.notes)
            .set(LOANS.CREATED_AT, now)
            .set(LOANS.MODIFIED_AT, now)
            .returning(LOANS.ID)
            .fetchOne()
            ?.get(LOANS.ID)
            ?: throw IllegalStateException("Failed to create loan")

        return fetchLoanById(authenticatedUser, loanId)
            ?: throw IllegalStateException("Created loan could not be retrieved")
    }

    override fun createIouLoan(
        authenticatedUser: UserDTO,
        contactId: UUID,
        sourceTransactionId: UUID,
        amount: BigDecimal,
        currency: Currency,
        description: String,
        loanDate: LocalDate,
        dueDate: LocalDate?,
        note: String?,
    ): LoanDTO {
        val now = OffsetDateTime.now()
        val loanId = dsl.insertInto(LOANS)
            .set(LOANS.USER_ID, authenticatedUser.id)
            .set(LOANS.CONTACT_ID, contactId)
            .set(LOANS.SOURCE_TRANSACTION_ID, sourceTransactionId)
            .set(LOANS.LENT_AMOUNT, amount)
            .set(LOANS.OWED_AMOUNT, amount)
            .set(LOANS.CURRENCY, currency.name)
            .set(LOANS.LOAN_DATE, loanDate)
            .set(LOANS.DESCRIPTION, description)
            .set(LOANS.DUE_DATE, dueDate)
            .set(LOANS.NOTES, note)
            .set(LOANS.CREATED_AT, now)
            .set(LOANS.MODIFIED_AT, now)
            .returning(LOANS.ID)
            .fetchOne()
            ?.get(LOANS.ID)
            ?: throw IllegalStateException("Failed to create IOU loan")

        return fetchLoanById(authenticatedUser, loanId)
            ?: throw IllegalStateException("Created IOU loan could not be retrieved")
    }

    override fun updateLoan(authenticatedUser: UserDTO, id: UUID, form: LoanUpdateForm): LoanDTO? {
        val updated = dsl.update(LOANS)
            .set(LOANS.OWED_AMOUNT, form.owedAmount)
            .set(LOANS.INTEREST_RATE, form.interestRate)
            .set(LOANS.DESCRIPTION, form.description)
            .set(LOANS.DUE_DATE, form.dueDate)
            .set(LOANS.NOTES, form.notes)
            .set(LOANS.MODIFIED_AT, OffsetDateTime.now())
            .where(LOANS.ID.eq(id).and(LOANS.USER_ID.eq(authenticatedUser.id)))
            .execute()
        return if (updated > 0) fetchLoanById(authenticatedUser, id) else null
    }

    override fun deleteLoan(authenticatedUser: UserDTO, id: UUID): Boolean {
        val record = dsl.select(LOANS.TRANSACTION_ID)
            .from(LOANS)
            .where(LOANS.USER_ID.eq(authenticatedUser.id).and(LOANS.ID.eq(id)))
            .fetchOne()
            ?: return false

        val loanTransactionId: UUID? = record[LOANS.TRANSACTION_ID]

        val lendingReversal: Pair<UUID, BigDecimal>? = loanTransactionId?.let { txId ->
            dsl.select(TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.AMOUNT)
                .from(TRANSACTIONS)
                .where(TRANSACTIONS.ID.eq(txId))
                .fetchOne()
                ?.let { it[TRANSACTIONS.ACCOUNT_ID]!! to (it[TRANSACTIONS.AMOUNT] ?: BigDecimal.ZERO) }
        }

        val repaymentTxRows = dsl.select(TRANSACTIONS.ID, TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.AMOUNT)
            .from(LOAN_REPAYMENTS)
            .join(TRANSACTIONS).on(TRANSACTIONS.ID.eq(LOAN_REPAYMENTS.TRANSACTION_ID))
            .where(LOAN_REPAYMENTS.LOAN_ID.eq(id))
            .fetch()
        val repaymentTransactionIds: List<UUID> = repaymentTxRows.map { it[TRANSACTIONS.ID]!! }

        dsl.deleteFrom(LOANS).where(LOANS.ID.eq(id)).execute()

        val txIdsToDelete = (repaymentTransactionIds + listOfNotNull(loanTransactionId)).distinct()
        if (txIdsToDelete.isNotEmpty()) {
            dsl.deleteFrom(TRANSACTIONS).where(TRANSACTIONS.ID.`in`(txIdsToDelete)).execute()
        }

        lendingReversal?.let { (accountId, amount) ->
            budgetAccountsRepository.updateBalance(accountId, amount, authenticatedUser)
        }
        repaymentTxRows.forEach { row ->
            val accountId = row[TRANSACTIONS.ACCOUNT_ID]!!
            val amount = row[TRANSACTIONS.AMOUNT] ?: BigDecimal.ZERO
            budgetAccountsRepository.updateBalance(accountId, amount.negate(), authenticatedUser)
        }

        return true
    }

    private fun fetchLoansWhere(condition: Condition): List<LoanDTO> {
        val repaidTotal = repaidTotalField()
        return dsl.select(
            LOANS.ID,
            LOANS.LENT_AMOUNT,
            LOANS.OWED_AMOUNT,
            LOANS.CURRENCY,
            LOANS.INTEREST_RATE,
            LOANS.LOAN_DATE,
            LOANS.DESCRIPTION,
            LOANS.DUE_DATE,
            LOANS.NOTES,
            LOANS.CREATED_AT,
            LOANS.MODIFIED_AT,
            LOANS.TRANSACTION_ID,
            LOANS.SOURCE_TRANSACTION_ID,
            CONTACTS.ID,
            CONTACTS.FIRST_NAME,
            CONTACTS.LAST_NAME,
            CONTACTS.PICTURE,
            TRANSACTIONS.ACCOUNT_ID,
            *TransactionRecordMapper.columns,
            repaidTotal,
        )
            .from(LOANS)
            .join(CONTACTS).on(CONTACTS.ID.eq(LOANS.CONTACT_ID))
            .leftJoin(TRANSACTIONS).on(TRANSACTIONS.ID.eq(LOANS.TRANSACTION_ID))
            .leftJoin(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))
            .where(condition)
            .orderBy(LOANS.LOAN_DATE.desc(), LOANS.CREATED_AT.desc())
            .fetch { mapToLoanDTO(it, repaidTotal) }
    }

    private fun repaidTotalField(): Field<BigDecimal> =
        DSL.coalesce(
            DSL.field(
                DSL.select(DSL.sum(LOAN_REPAYMENTS.AMOUNT))
                    .from(LOAN_REPAYMENTS)
                    .where(LOAN_REPAYMENTS.LOAN_ID.eq(LOANS.ID))
            ),
            BigDecimal.ZERO
        ).`as`("repaid_total")

    private fun mapToLoanDTO(record: Record, repaidTotal: Field<BigDecimal>): LoanDTO {
        val owed = record[LOANS.OWED_AMOUNT] ?: BigDecimal.ZERO
        val repaid = record[repaidTotal] ?: BigDecimal.ZERO
        val first = record[CONTACTS.FIRST_NAME] ?: ""
        val last = record[CONTACTS.LAST_NAME]

        val contact = ContactDTO(
            id = record[CONTACTS.ID],
            firstName = first,
            lastName = last,
            name = if (last.isNullOrBlank()) first else "$first $last",
            picture = record[CONTACTS.PICTURE],
        )

        val transaction: TransactionDTO? = TransactionRecordMapper.mapTransactionOrNull(record)

        return LoanDTO(
            id = record[LOANS.ID],
            contact = contact,
            transaction = transaction,
            sourceTransactionId = record[LOANS.SOURCE_TRANSACTION_ID],
            accountId = record[TRANSACTIONS.ACCOUNT_ID],
            affectsBalance = transaction != null,
            lentAmount = record[LOANS.LENT_AMOUNT] ?: BigDecimal.ZERO,
            owedAmount = owed,
            totalRepaid = repaid,
            outstanding = owed - repaid,
            currency = record[LOANS.CURRENCY] ?: "EUR",
            interestRate = record[LOANS.INTEREST_RATE],
            loanDate = record[LOANS.LOAN_DATE],
            description = record[LOANS.DESCRIPTION],
            dueDate = record[LOANS.DUE_DATE],
            notes = record[LOANS.NOTES],
            createdAt = record[LOANS.CREATED_AT],
            modifiedAt = record[LOANS.MODIFIED_AT],
        )
    }
}
