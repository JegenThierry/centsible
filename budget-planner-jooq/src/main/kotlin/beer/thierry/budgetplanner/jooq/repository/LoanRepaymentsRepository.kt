package beer.thierry.budgetplanner.jooq.repository

import beer.thierry.budgetplanner.api.model.category.CategoryDTO
import beer.thierry.budgetplanner.api.model.category.CategoryType
import beer.thierry.budgetplanner.api.model.loan.RepaymentDTO
import beer.thierry.budgetplanner.api.model.loan.RepaymentForm
import beer.thierry.budgetplanner.api.model.transaction.TransactionDTO
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.IBudgetAccountsRepository
import beer.thierry.budgetplanner.api.repository.ILoanRepaymentsRepository
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.LOANS
import beer.thierry.jooq.generated.tables.references.LOAN_REPAYMENTS
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class LoanRepaymentsRepository(
    private val dsl: DSLContext,
    private val budgetAccountsRepository: IBudgetAccountsRepository,
) : ILoanRepaymentsRepository {

    override fun fetchRepayments(authenticatedUser: UserDTO, loanId: UUID): List<RepaymentDTO> =
        baseSelect()
            .where(LOANS.USER_ID.eq(authenticatedUser.id).and(LOAN_REPAYMENTS.LOAN_ID.eq(loanId)))
            .orderBy(LOAN_REPAYMENTS.REPAID_AT.desc(), LOAN_REPAYMENTS.CREATED_AT.desc())
            .fetch { mapToDTO(it) }

    private fun fetchRepaymentById(authenticatedUser: UserDTO, repaymentId: UUID): RepaymentDTO? =
        baseSelect()
            .where(LOANS.USER_ID.eq(authenticatedUser.id).and(LOAN_REPAYMENTS.ID.eq(repaymentId)))
            .fetchOne { mapToDTO(it) }

    private fun baseSelect() = dsl.select(
        LOAN_REPAYMENTS.ID,
        LOAN_REPAYMENTS.LOAN_ID,
        LOAN_REPAYMENTS.AMOUNT,
        LOAN_REPAYMENTS.REPAID_AT,
        LOAN_REPAYMENTS.CREATED_AT,
        LOAN_REPAYMENTS.TRANSACTION_ID,
        TRANSACTIONS.ID,
        TRANSACTIONS.ACCOUNT_ID,
        TRANSACTIONS.AMOUNT,
        TRANSACTIONS.DESCRIPTION,
        TRANSACTIONS.TRANSACTION_DATE,
        TRANSACTIONS.CREATED_AT,
        TRANSACTIONS.MODIFIED_AT,
        CATEGORIES.ID,
        CATEGORIES.NAME,
        CATEGORIES.ICON,
        CATEGORIES.COLOR,
        CATEGORIES.TYPE,
    )
        .from(LOAN_REPAYMENTS)
        .join(LOANS).on(LOANS.ID.eq(LOAN_REPAYMENTS.LOAN_ID))
        .leftJoin(TRANSACTIONS).on(TRANSACTIONS.ID.eq(LOAN_REPAYMENTS.TRANSACTION_ID))
        .leftJoin(CATEGORIES).on(CATEGORIES.ID.eq(TRANSACTIONS.CATEGORY_ID))

    override fun createRepayment(authenticatedUser: UserDTO, loanId: UUID, form: RepaymentForm): RepaymentDTO {
        val now = OffsetDateTime.now()
        val description = form.description?.takeIf { it.isNotBlank() } ?: "Repayment"

        val transactionId: UUID? = if (form.affectBalance) {
            val accountId = form.accountId
                ?: throw IllegalArgumentException("Account is required when the repayment affects an account balance.")
            dsl.ensureAccountOwnedByUser(accountId, authenticatedUser.id)

            val repaymentCategoryId = dsl.findManagedCategoryId(ManagedCategoryNames.REPAYMENT)
                ?: throw IllegalStateException("System Repayment category not found. Migration may not have run.")

            val newTransactionId = dsl.insertInto(TRANSACTIONS)
                .set(TRANSACTIONS.ACCOUNT_ID, accountId)
                .set(TRANSACTIONS.CATEGORY_ID, repaymentCategoryId)
                .set(TRANSACTIONS.AMOUNT, form.amount)
                .set(TRANSACTIONS.DESCRIPTION, description)
                .set(TRANSACTIONS.TRANSACTION_DATE, form.repaidAt)
                .set(TRANSACTIONS.CREATED_AT, now)
                .set(TRANSACTIONS.MODIFIED_AT, now)
                .returning(TRANSACTIONS.ID)
                .fetchOne()
                ?.get(TRANSACTIONS.ID)
                ?: throw IllegalStateException("Failed to create repayment transaction")

            // Repayment is an INCOME-typed managed category, so the cash returns to the account.
            budgetAccountsRepository.updateBalance(accountId, form.amount)

            newTransactionId
        } else null

        val repaymentId = dsl.insertInto(LOAN_REPAYMENTS)
            .set(LOAN_REPAYMENTS.LOAN_ID, loanId)
            .set(LOAN_REPAYMENTS.TRANSACTION_ID, transactionId)
            .set(LOAN_REPAYMENTS.AMOUNT, form.amount)
            .set(LOAN_REPAYMENTS.REPAID_AT, form.repaidAt)
            .set(LOAN_REPAYMENTS.CREATED_AT, now)
            .returning(LOAN_REPAYMENTS.ID)
            .fetchOne()
            ?.get(LOAN_REPAYMENTS.ID)
            ?: throw IllegalStateException("Failed to create repayment")

        return fetchRepaymentById(authenticatedUser, repaymentId)
            ?: throw IllegalStateException("Created repayment could not be retrieved")
    }

    override fun deleteRepayment(authenticatedUser: UserDTO, loanId: UUID, repaymentId: UUID): Boolean {
        val record = dsl.select(LOAN_REPAYMENTS.TRANSACTION_ID)
            .from(LOAN_REPAYMENTS)
            .join(LOANS).on(LOANS.ID.eq(LOAN_REPAYMENTS.LOAN_ID))
            .where(
                LOANS.USER_ID.eq(authenticatedUser.id)
                    .and(LOAN_REPAYMENTS.LOAN_ID.eq(loanId))
                    .and(LOAN_REPAYMENTS.ID.eq(repaymentId))
            )
            .fetchOne()
            ?: return false

        val transactionId = record[LOAN_REPAYMENTS.TRANSACTION_ID]

        // Look up the (accountId, amount) pair on the linked tx before deleting it,
        // so the original INCOME-typed balance bump can be undone.
        val reversal: Pair<UUID, BigDecimal>? = transactionId?.let { txId ->
            dsl.select(TRANSACTIONS.ACCOUNT_ID, TRANSACTIONS.AMOUNT)
                .from(TRANSACTIONS)
                .where(TRANSACTIONS.ID.eq(txId))
                .fetchOne()
                ?.let { it[TRANSACTIONS.ACCOUNT_ID]!! to (it[TRANSACTIONS.AMOUNT] ?: BigDecimal.ZERO) }
        }

        val deleted = if (transactionId != null) {
            // Deleting the transaction cascades to the repayment row.
            dsl.deleteFrom(TRANSACTIONS).where(TRANSACTIONS.ID.eq(transactionId)).execute() > 0
        } else {
            dsl.deleteFrom(LOAN_REPAYMENTS).where(LOAN_REPAYMENTS.ID.eq(repaymentId)).execute() > 0
        }

        if (deleted) {
            reversal?.let { (accountId, amount) ->
                budgetAccountsRepository.updateBalance(accountId, amount.negate())
            }
        }

        return deleted
    }

    override fun totalRepaidForLoan(loanId: UUID): BigDecimal {
        return dsl.select(DSL.coalesce(DSL.sum(LOAN_REPAYMENTS.AMOUNT), BigDecimal.ZERO))
            .from(LOAN_REPAYMENTS)
            .where(LOAN_REPAYMENTS.LOAN_ID.eq(loanId))
            .fetchOne()
            ?.value1()
            ?: BigDecimal.ZERO
    }

    private fun mapToDTO(record: Record): RepaymentDTO {
        val txnId = record[LOAN_REPAYMENTS.TRANSACTION_ID]
        val transaction: TransactionDTO? = if (txnId != null) {
            val category = CategoryDTO(
                id = record[CATEGORIES.ID],
                name = record[CATEGORIES.NAME],
                icon = record[CATEGORIES.ICON],
                color = record[CATEGORIES.COLOR],
                type = record[CATEGORIES.TYPE]?.let { CategoryType.fromValue(it) } ?: CategoryType.INCOME,
                isSystem = true
            )
            TransactionDTO(
                id = record[TRANSACTIONS.ID],
                category = category,
                amount = record[TRANSACTIONS.AMOUNT],
                description = record[TRANSACTIONS.DESCRIPTION],
                transactionDate = record[TRANSACTIONS.TRANSACTION_DATE],
                createdAt = record[TRANSACTIONS.CREATED_AT],
                updatedAt = record[TRANSACTIONS.MODIFIED_AT],
            )
        } else null

        return RepaymentDTO(
            id = record[LOAN_REPAYMENTS.ID],
            loanId = record[LOAN_REPAYMENTS.LOAN_ID],
            transaction = transaction,
            affectsBalance = transaction != null,
            amount = record[LOAN_REPAYMENTS.AMOUNT] ?: BigDecimal.ZERO,
            repaidAt = record[LOAN_REPAYMENTS.REPAID_AT],
            createdAt = record[LOAN_REPAYMENTS.CREATED_AT],
        )
    }
}
