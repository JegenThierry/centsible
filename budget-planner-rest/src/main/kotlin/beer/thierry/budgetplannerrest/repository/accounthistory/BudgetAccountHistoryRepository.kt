package beer.thierry.budgetplannerrest.repository.accounthistory

import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.jooq.generated.tables.references.ACCOUNT_HISTORY
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class BudgetAccountsRepository(private val dsl: DSLContext) : IBudgetAccountHistoryRepository {

    override fun fetchCompleteAccountHistory(
        accountId: UUID, authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO> =
        dsl.select(ACCOUNT_HISTORY.ID, ACCOUNT_HISTORY.ACCOUNT_ID, ACCOUNT_HISTORY.BALANCE, ACCOUNT_HISTORY.CREATED_AT)
            .from(ACCOUNT_HISTORY)
            .where(ACCOUNT_HISTORY.ACCOUNT_ID.eq(accountId).and(ACCOUNT_HISTORY.USER_ID.eq(authenticatedUser.id)))
            .fetchInto(BudgetAccountSnapshotDTO::class.java)


    override fun fetchAccountHistory(
        accountId: UUID, startDate: OffsetDateTime, endDate: OffsetDateTime, authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO> =
        dsl.select(ACCOUNT_HISTORY.ID, ACCOUNT_HISTORY.ACCOUNT_ID, ACCOUNT_HISTORY.BALANCE, ACCOUNT_HISTORY.CREATED_AT)
            .from(ACCOUNT_HISTORY)
            .where(
                ACCOUNT_HISTORY.ACCOUNT_ID.eq(accountId)
                    .and(ACCOUNT_HISTORY.USER_ID.eq(authenticatedUser.id))
                    .and(ACCOUNT_HISTORY.CREATED_AT.between(startDate, endDate))
            )
            .fetchInto(BudgetAccountSnapshotDTO::class.java)

}
