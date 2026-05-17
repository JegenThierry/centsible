package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.OffsetDateTime

interface IReportsRepository {
    fun fetchAllUserSnapshotsUntil(
        until: OffsetDateTime,
        authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO>
}
