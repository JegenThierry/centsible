package beer.thierry.centsible.core.services.notifications

import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.notification.NotificationType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.repository.INotificationRepository
import beer.thierry.centsible.api.services.notifications.INotificationService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.util.*

private val THRESHOLD = BigDecimal("0.85")
private val EXCEEDED = BigDecimal("1.00")

@Service
class NotificationService(
    private val notifications: INotificationRepository,
    private val budgets: IBudgetRepository,
) : INotificationService {

    override fun list(user: UserDTO, limit: Int): List<NotificationDTO> = notifications.list(user, limit)

    override fun countUnread(user: UserDTO): Int = notifications.countUnread(user)

    override fun markRead(id: UUID, user: UserDTO): Boolean = notifications.markRead(id, user)

    override fun markAllRead(user: UserDTO): Int = notifications.markAllRead(user)

    override fun delete(id: UUID, user: UserDTO): Boolean = notifications.delete(id, user)

    override fun maybeRaiseBudgetAlerts(user: UserDTO) {
        val now = YearMonth.now()
        val list = budgets.fetchAllWithSpentForMonth(user, now)
        for (b in list) {
            val limit = b.amountLimit + b.rolloverAmount
            if (limit.signum() <= 0) continue

            val ratio = b.amountSpent.divide(limit, 4, RoundingMode.HALF_UP)
            val budgetId = b.id?.toString() ?: continue
            val periodKey = b.period ?: continue
            val categoryName = b.category.name ?: ""

            if (ratio >= EXCEEDED) {
                if (!notifications.hasRecent(user, NotificationType.BUDGET_EXCEEDED, budgetId, periodKey)) {
                    notifications.create(
                        user,
                        NotificationType.BUDGET_EXCEEDED,
                        "Budget exceeded: $categoryName",
                        "You've spent more than your $categoryName budget for this period.",
                        mapOf(
                            "budgetId" to budgetId,
                            "periodKey" to periodKey,
                            "categoryName" to categoryName,
                            "spent" to b.amountSpent.toPlainString(),
                            "limit" to limit.toPlainString(),
                        ),
                    )
                }
            } else if (ratio >= THRESHOLD) {
                if (!notifications.hasRecent(user, NotificationType.BUDGET_THRESHOLD, budgetId, periodKey)) {
                    val percent = ratio.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toPlainString()
                    notifications.create(
                        user,
                        NotificationType.BUDGET_THRESHOLD,
                        "Approaching budget: $categoryName",
                        "You're at $percent% of your $categoryName budget for this period.",
                        mapOf(
                            "budgetId" to budgetId,
                            "periodKey" to periodKey,
                            "categoryName" to categoryName,
                            "percent" to percent,
                        ),
                    )
                }
            }
        }
    }
}
