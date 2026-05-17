package beer.thierry.centsible.core.services.notifications

import beer.thierry.centsible.api.model.budget.BudgetDTO
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

    override fun maybeRaiseBudgetAlerts(user: UserDTO, categoryIds: Collection<Long>?) {
        val all = budgets.fetchAllWithSpentForMonth(user, YearMonth.now())
        val scoped = if (categoryIds == null) all else all.filter { it.category.id in categoryIds }
        scoped.forEach { evaluate(user, it) }
    }

    private fun evaluate(user: UserDTO, b: BudgetDTO) {
        val limit = b.amountLimit + b.rolloverAmount
        if (limit.signum() <= 0) return

        val ratio = b.amountSpent.divide(limit, 4, RoundingMode.HALF_UP)
        val budgetId = b.id?.toString() ?: return
        val periodKey = b.period ?: return
        val category = b.category.name ?: ""

        when {
            ratio >= EXCEEDED -> raise(
                user, NotificationType.BUDGET_EXCEEDED, budgetId, periodKey,
                title = "Budget exceeded: $category",
                body = "You've spent more than your $category budget for this period.",
                extras = mapOf("categoryName" to category, "spent" to b.amountSpent.toPlainString(), "limit" to limit.toPlainString()),
            )
            ratio >= THRESHOLD -> {
                val percent = ratio.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toPlainString()
                raise(
                    user, NotificationType.BUDGET_THRESHOLD, budgetId, periodKey,
                    title = "Approaching budget: $category",
                    body = "You're at $percent% of your $category budget for this period.",
                    extras = mapOf("categoryName" to category, "percent" to percent),
                )
            }
        }
    }

    private fun raise(
        user: UserDTO,
        type: NotificationType,
        budgetId: String,
        periodKey: String,
        title: String,
        body: String,
        extras: Map<String, String>,
    ) {
        if (notifications.hasRecent(user, type, budgetId, periodKey)) return
        notifications.create(user, type, title, body, mapOf("budgetId" to budgetId, "periodKey" to periodKey) + extras)
    }
}
