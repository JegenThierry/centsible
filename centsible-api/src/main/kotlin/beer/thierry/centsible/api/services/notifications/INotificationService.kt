package beer.thierry.centsible.api.services.notifications

import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.util.UUID

interface INotificationService {
    fun list(user: UserDTO, limit: Int = 50): List<NotificationDTO>
    fun countUnread(user: UserDTO): Int
    fun markRead(id: UUID, user: UserDTO): Boolean
    fun markAllRead(user: UserDTO): Int
    fun delete(id: UUID, user: UserDTO): Boolean

    /**
     * Inspects budgets for [user] and emits notifications when thresholds are crossed. Idempotent per
     * budget+period. If [categoryIds] is non-null, only budgets for those categories are evaluated.
     */
    fun maybeRaiseBudgetAlerts(user: UserDTO, categoryIds: Collection<Long>? = null)

    /**
     * Evaluates a transaction's amount against the user's large-transaction threshold and raises a
     * one-shot notification if it exceeds it. Idempotent per transaction.
     */
    fun maybeRaiseLargeTransactionAlert(user: UserDTO, transactionId: UUID, amount: BigDecimal, description: String?)

    /**
     * Evaluates the user's account balances against their low-balance threshold and raises alerts.
     * Called both inline after a transaction and from the scheduled sweep.
     */
    fun maybeRaiseLowBalanceAlerts(user: UserDTO, accountIds: Collection<UUID>? = null)

    /**
     * Combined inline post-write hook: runs large-transaction + low-balance checks on a single
     * settings fetch. Prefer this over calling the two individual methods back-to-back from the
     * transaction write path.
     */
    fun evaluateTransactionAlerts(
        user: UserDTO,
        transactionId: UUID,
        amount: BigDecimal,
        description: String?,
        accountId: UUID,
    )

    /**
     * Runs all scheduled checks (loan due, recurring upcoming, low balance, provider consent expiry)
     * for [user]. Safe to call repeatedly — each individual check is idempotent via dedup keys.
     */
    fun runScheduledChecks(user: UserDTO)

    /**
     * Raises an alert that a provider connection's automated sync failed. Deduplicated per connection
     * per day, so a persistently-failing connection alerts at most once a day rather than every poll.
     */
    fun maybeRaiseSyncFailure(user: UserDTO, connectionId: UUID, provider: String, error: String?)
}
