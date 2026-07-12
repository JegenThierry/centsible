package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.budgetaccount.UpdateBudgetAccountRequest
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.util.*

interface IBudgetAccountsRepository {
    fun fetchAllAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO>

    /** Throws [beer.thierry.centsible.api.exceptions.LocalizedException.NotFound] when [id] is missing or not owned by [authenticatedUser]. */
    fun fetchAccountById(id: UUID, authenticatedUser: UserDTO): BudgetAccountDTO
    fun createAccount(
        authenticatedUser: UserDTO,
        createBudgetAccountRequest: CreateBudgetAccountRequest
    ): BudgetAccountDTO

    /** Updates the editable fields (name, type) scoped to [authenticatedUser]; throws [beer.thierry.centsible.api.exceptions.LocalizedException.NotFound] when [id] is missing or not owned. */
    fun updateAccount(
        id: UUID,
        updateBudgetAccountRequest: UpdateBudgetAccountRequest,
        authenticatedUser: UserDTO
    ): BudgetAccountDTO

    /** Deletes the account scoped to [authenticatedUser] (transactions/recurring cascade); throws [beer.thierry.centsible.api.exceptions.LocalizedException.NotFound] when [id] is missing or not owned. */
    fun deleteAccount(id: UUID, authenticatedUser: UserDTO)

    fun fetchInitialBalance(accountId: UUID, authenticatedUser: UserDTO): BigDecimal
    fun updateBalance(accountId: UUID, amount: BigDecimal, authenticatedUser: UserDTO)

    /** Internal cross-domain lookup, intentionally not ownership-scoped; callers must have already verified the account. */
    fun fetchAccountCurrency(accountId: UUID): Currency
}
