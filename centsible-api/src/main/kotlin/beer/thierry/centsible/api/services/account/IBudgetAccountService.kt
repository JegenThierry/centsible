package beer.thierry.centsible.api.services.account

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.centsible.api.model.budgetaccount.UpdateBudgetAccountRequest
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.LocalDate

interface IBudgetAccountService {
    fun createAccount(
        createBudgetAccountRequest: CreateBudgetAccountRequest,
        authenticatedUser: UserDTO
    ): BudgetAccountDTO

    fun updateAccount(
        id: String,
        updateBudgetAccountRequest: UpdateBudgetAccountRequest,
        authenticatedUser: UserDTO
    ): BudgetAccountDTO

    fun deleteAccount(id: String, authenticatedUser: UserDTO)

    fun fetchAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO>
    fun fetchAccountById(id: String, authenticatedUser: UserDTO): BudgetAccountDTO
    fun fetchAccountSnapshots(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO>
}
