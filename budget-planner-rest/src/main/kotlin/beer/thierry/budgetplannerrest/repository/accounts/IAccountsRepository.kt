package beer.thierry.budgetplannerrest.repository.accounts

import beer.thierry.budgetplannerrest.model.account.AccountDTO
import beer.thierry.budgetplannerrest.model.account.CreateAccountRequest
import beer.thierry.budgetplannerrest.model.user.User
import beer.thierry.budgetplannerrest.model.user.UserDTO

interface IAccountsRepository {
    fun fetchAllAccounts(authenticatedUser: UserDTO): List<AccountDTO>
    fun createAccount(authenticatedUser: UserDTO, createAccountRequest: CreateAccountRequest): AccountDTO
}