package beer.thierry.budgetplannerrest.repository.accounts

import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class BudgetAccountsRepository(private val dsl: DSLContext) : IBudgetAccountsRepository {
    override fun fetchAllAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO> {
        return dsl.select(
            ACCOUNTS.ID,
            ACCOUNTS.NAME,
            ACCOUNTS.BALANCE,
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
            .set(ACCOUNTS.CURRENCY, createBudgetAccountRequest.currency.toString())
            .set(ACCOUNTS.CREATED_AT, OffsetDateTime.now())
            .set(ACCOUNTS.MODIFIED_AT, OffsetDateTime.now())
            .returning(ACCOUNTS.ID, ACCOUNTS.NAME, ACCOUNTS.BALANCE, ACCOUNTS.CURRENCY)
            .fetchOneInto(BudgetAccountDTO::class.java)
            ?: throw IllegalStateException("Failed to retrieve generated Account")
    }
}