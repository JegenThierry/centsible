package beer.thierry.budgetplannerrest.repository.accounts

import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplannerrest.model.user.UserDTO
import java.math.BigDecimal
import java.util.UUID

interface IBudgetAccountsRepository {
    fun fetchAllAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO>
    fun fetchAccountById(id: UUID, authenticatedUser: UserDTO): BudgetAccountDTO
    fun createAccount(authenticatedUser: UserDTO, createBudgetAccountRequest: CreateBudgetAccountRequest): BudgetAccountDTO
    fun fetchInitialBalance(accountId: UUID, authenticatedUser: UserDTO): BigDecimal
}
