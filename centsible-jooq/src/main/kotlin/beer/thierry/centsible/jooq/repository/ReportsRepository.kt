package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IReportsRepository
import beer.thierry.jooq.generated.tables.references.ACCOUNT_HISTORY
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class ReportsRepository(private val dsl: DSLContext) : IReportsRepository {

    override fun fetchAllUserSnapshotsUntil(
        until: OffsetDateTime,
        authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO> =
        dsl.select(
            ACCOUNT_HISTORY.ACCOUNT_ID,
            ACCOUNT_HISTORY.BALANCE,
            ACCOUNT_HISTORY.CREATED_AT,
        )
            .from(ACCOUNT_HISTORY)
            .where(
                ACCOUNT_HISTORY.USER_ID.eq(authenticatedUser.id)
                    .and(ACCOUNT_HISTORY.CREATED_AT.le(until))
            )
            .orderBy(ACCOUNT_HISTORY.CREATED_AT.asc())
            .fetchInto(BudgetAccountSnapshotDTO::class.java)
}
