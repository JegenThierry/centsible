package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.budgetplanner.api.model.user.UserDTO
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
}
