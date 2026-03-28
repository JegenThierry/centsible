package beer.thierry.budgetplannerrest.repository.accounts

import beer.thierry.budgetplannerrest.model.account.AccountDTO
import beer.thierry.budgetplannerrest.model.account.CreateAccountRequest
import beer.thierry.budgetplannerrest.model.user.User
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.jooq.generated.tables.references.ACCOUNTS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime

@Repository
class AccountsRepository(private val dsl: DSLContext) : IAccountsRepository {
    override fun fetchAllAccounts(authenticatedUser: UserDTO): List<AccountDTO> {
        return dsl.select(
            ACCOUNTS.ID,
            ACCOUNTS.NAME,
            ACCOUNTS.BALANCE,
            ACCOUNTS.CURRENCY,
            ACCOUNTS.CREATED_AT,
            ACCOUNTS.MODIFIED_AT
        )
            .from(ACCOUNTS)
            .where(ACCOUNTS.USER_ID.eq(authenticatedUser.id))
            .fetchInto(AccountDTO::class.java)
    }

    override fun createAccount(
        authenticatedUser: UserDTO,
        createAccountRequest: CreateAccountRequest
    ): AccountDTO {
        return dsl.insertInto(ACCOUNTS)
            .set(ACCOUNTS.USER_ID, authenticatedUser.id)
            .set(ACCOUNTS.NAME, createAccountRequest.name)
            .set(ACCOUNTS.BALANCE, createAccountRequest.initialBalance as BigDecimal)
            .set(ACCOUNTS.CURRENCY, createAccountRequest.currency.toString())
            .set(ACCOUNTS.CREATED_AT, OffsetDateTime.now())
            .set(ACCOUNTS.MODIFIED_AT, OffsetDateTime.now())
            .returning(ACCOUNTS.ID)
            .fetchOneInto(AccountDTO::class.java)
            ?: throw IllegalStateException("Failed to retrieve generated Account")
    }
}