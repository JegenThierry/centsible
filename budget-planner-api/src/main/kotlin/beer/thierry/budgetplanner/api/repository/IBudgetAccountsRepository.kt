package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.math.BigDecimal
import java.util.*

interface IBudgetAccountsRepository {
    fun fetchAllAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO>
    fun fetchAccountById(id: UUID, authenticatedUser: UserDTO): BudgetAccountDTO
    fun createAccount(
        authenticatedUser: UserDTO,
        createBudgetAccountRequest: CreateBudgetAccountRequest
    ): BudgetAccountDTO

    fun fetchInitialBalance(accountId: UUID, authenticatedUser: UserDTO): BigDecimal
    fun updateBalance(accountId: UUID, amount: BigDecimal)
}
