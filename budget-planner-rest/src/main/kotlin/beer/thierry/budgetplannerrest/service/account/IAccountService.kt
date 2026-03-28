package beer.thierry.budgetplannerrest.service.account

import beer.thierry.budgetplannerrest.model.account.AccountDTO
import beer.thierry.budgetplannerrest.model.account.CreateAccountRequest
import beer.thierry.budgetplannerrest.model.user.User
import beer.thierry.budgetplannerrest.model.user.UserDTO

interface IAccountService {
    fun createAccount(authenticatedUser: UserDTO, createAccountRequest: CreateAccountRequest): AccountDTO
    fun fetchAccounts(authenticatedUser: UserDTO): List<AccountDTO>
}