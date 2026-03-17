package beer.thierry.budgetplannerrest.repository.accounts

import beer.thierry.budgetplannerrest.model.account.AccountDTO
import beer.thierry.budgetplannerrest.model.account.CreateAccountRequest
import beer.thierry.budgetplannerrest.model.user.User

interface IAccountsRepository {
    fun fetchAllAccounts(authenticatedUser: User): List<AccountDTO>
    fun createAccount(authenticatedUser: User, createAccountRequest: CreateAccountRequest): AccountDTO
}