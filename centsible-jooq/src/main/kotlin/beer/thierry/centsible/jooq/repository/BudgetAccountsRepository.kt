package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Repository
class BudgetAccountsRepository(private val dsl: DSLContext) : IBudgetAccountsRepository {
    override fun fetchAllAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO> {
        return dsl.select(
            ACCOUNTS.ID,
            ACCOUNTS.NAME,
            ACCOUNTS.BALANCE,
            ACCOUNTS.INITIAL_BALANCE,
            ACCOUNTS.CURRENCY,
        )
            .from(ACCOUNTS)
            .where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
            .fetchInto(BudgetAccountDTO::class.java)
    }

    override fun fetchAccountById(id: UUID, authenticatedUser: UserDTO): BudgetAccountDTO {
        return dsl.select(
            ACCOUNTS.ID,
            ACCOUNTS.NAME,
            ACCOUNTS.BALANCE,
            ACCOUNTS.INITIAL_BALANCE,
            ACCOUNTS.CURRENCY,
        )
            .from(ACCOUNTS)
            .where(ACCOUNTS.USER_ID.eq(authenticatedUser.id).and(ACCOUNTS.ID.eq(id)))
            .fetchSingleInto(BudgetAccountDTO::class.java)
    }

    override fun createAccount(
        authenticatedUser: UserDTO,
        createBudgetAccountRequest: CreateBudgetAccountRequest
    ): BudgetAccountDTO {
        return dsl.insertInto(ACCOUNTS)
            .set(ACCOUNTS.USER_ID, authenticatedUser.id)
            .set(ACCOUNTS.NAME, createBudgetAccountRequest.name)
            .set(ACCOUNTS.BALANCE, createBudgetAccountRequest.initialBalance)
            .set(ACCOUNTS.INITIAL_BALANCE, createBudgetAccountRequest.initialBalance)
            .set(ACCOUNTS.CURRENCY, createBudgetAccountRequest.currency.toString())
            .set(ACCOUNTS.CREATED_AT, OffsetDateTime.now())
            .set(ACCOUNTS.MODIFIED_AT, OffsetDateTime.now())
            .returning(ACCOUNTS.ID, ACCOUNTS.NAME, ACCOUNTS.BALANCE, ACCOUNTS.INITIAL_BALANCE, ACCOUNTS.CURRENCY)
            .fetchOneInto(BudgetAccountDTO::class.java)
            ?: throw IllegalStateException("Failed to retrieve generated Account")
    }

    override fun fetchInitialBalance(accountId: UUID, authenticatedUser: UserDTO): BigDecimal {
        val account = dsl.select(ACCOUNTS.ID, ACCOUNTS.field("initial_balance", BigDecimal::class.java))
            .from(ACCOUNTS)
            .where(ACCOUNTS.ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
            .fetchOne() ?: throw IllegalArgumentException("Account not found")

        return account.get("initial_balance", BigDecimal::class.java) ?: BigDecimal.ZERO
    }

    override fun updateBalance(accountId: UUID, amount: BigDecimal, authenticatedUser: UserDTO) {
        val rows = dsl.update(ACCOUNTS)
            .set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.plus(amount))
            .set(ACCOUNTS.MODIFIED_AT, OffsetDateTime.now())
            .where(ACCOUNTS.ID.eq(accountId).and(ACCOUNTS.USER_ID.eq(authenticatedUser.id)))
            .execute()
        // Must throw, not no-op: @Transactional callers rely on this to roll back.
        if (rows == 0) throw IllegalArgumentException("Account not found or not owned by user")
    }
}
