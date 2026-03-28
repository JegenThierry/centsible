package beer.thierry.budgetplannerrest.service.account

import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplannerrest.model.user.UserDTO

interface IBudgetAccountService {
    fun createAccount(createBudgetAccountRequest: CreateBudgetAccountRequest, authenticatedUser: UserDTO): BudgetAccountDTO
    fun fetchAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO>
    fun fetchAccountById(id: String, authenticatedUser: UserDTO): BudgetAccountDTO
}