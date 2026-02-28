package beer.thierry.budgetplannerrest.service.account

import beer.thierry.budgetplannerrest.model.account.CreateAccountRequest

interface IAccountService {
    fun createAccount(createAccountRequest: CreateAccountRequest)
}