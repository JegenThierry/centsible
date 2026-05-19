package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.notification.NotificationType
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.*

interface INotificationRepository {
    fun create(
        user: UserDTO,
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): NotificationDTO

    fun list(user: UserDTO, limit: Int = 50): List<NotificationDTO>

    fun countUnread(user: UserDTO): Int

    fun markRead(id: UUID, user: UserDTO): Boolean

    fun markAllRead(user: UserDTO): Int

    fun delete(id: UUID, user: UserDTO): Boolean

    /**
     * True iff a notification of [type] with data.dedupKey == [dedupKey] already exists for [user].
     * Used to make alert emission idempotent across scheduler runs and inline triggers.
     */
    fun existsByDedupKey(user: UserDTO, type: NotificationType, dedupKey: String): Boolean

    /** Legacy budget-specific helper. Prefer [existsByDedupKey] for new alert types. */
    fun hasRecent(user: UserDTO, type: NotificationType, budgetId: String, sincePeriodKey: String): Boolean
}
