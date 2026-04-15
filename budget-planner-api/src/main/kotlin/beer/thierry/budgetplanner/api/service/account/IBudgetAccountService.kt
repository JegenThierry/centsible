package beer.thierry.budgetplanner.api.service.account

import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.time.LocalDate

interface IBudgetAccountService {
    fun createAccount(createBudgetAccountRequest: CreateBudgetAccountRequest, authenticatedUser: UserDTO): BudgetAccountDTO
    fun fetchAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO>
    fun fetchAccountById(id: String, authenticatedUser: UserDTO): BudgetAccountDTO
    fun fetchAccountSnapshots(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO>
}
