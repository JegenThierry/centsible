package beer.thierry.centsible.api.services.notifications

import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.*

interface INotificationService {
    fun list(user: UserDTO, limit: Int = 50): List<NotificationDTO>
    fun countUnread(user: UserDTO): Int
    fun markRead(id: UUID, user: UserDTO): Boolean
    fun markAllRead(user: UserDTO): Int
    fun delete(id: UUID, user: UserDTO): Boolean

    /** Inspects budgets for [user] and emits notifications when thresholds are crossed. Idempotent per budget+period. */
    fun maybeRaiseBudgetAlerts(user: UserDTO)
}
