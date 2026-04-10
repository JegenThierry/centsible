package beer.thierry.budgetplannerrest.repository.accounthistory

import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.budgetplannerrest.model.user.UserDTO
import java.time.OffsetDateTime
import java.util.UUID

interface IBudgetAccountHistoryRepository {
    fun fetchCompleteAccountHistory(accountId: UUID, authenticatedUser: UserDTO): List<BudgetAccountSnapshotDTO>
    fun fetchAccountHistory(
        accountId: UUID,
        startDate: OffsetDateTime,
        endDate: OffsetDateTime,
        authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO>

    fun logAccountHistory(account: BudgetAccountDTO, authenticatedUser: UserDTO)
}
