package beer.thierry.centsible.core.services.notifications

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
import beer.thierry.centsible.api.model.notification.NotificationType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.repository.ILoansRepository
import beer.thierry.centsible.api.repository.INotificationRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.notifications.INotificationService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

private val THRESHOLD = BigDecimal("0.85")
private val EXCEEDED = BigDecimal("1.00")

@Service
class NotificationService(
    private val notifications: INotificationRepository,
    private val budgets: IBudgetRepository,
    private val users: IUserRepository,
    private val loans: ILoansRepository,
    private val recurring: IRecurringTransactionRepository,
    private val accounts: IBudgetAccountsRepository,
) : INotificationService {

    override fun list(user: UserDTO, limit: Int): List<NotificationDTO> = notifications.list(user, limit)

    override fun countUnread(user: UserDTO): Int = notifications.countUnread(user)

    override fun markRead(id: UUID, user: UserDTO): Boolean = notifications.markRead(id, user)

    override fun markAllRead(user: UserDTO): Int = notifications.markAllRead(user)

    override fun delete(id: UUID, user: UserDTO): Boolean = notifications.delete(id, user)

    override fun maybeRaiseBudgetAlerts(user: UserDTO, categoryIds: Collection<Long>?) {
        val all = budgets.fetchAllWithSpentForMonth(user, YearMonth.now())
        val scoped = if (categoryIds == null) all else all.filter { it.category.id in categoryIds }
        scoped.forEach { evaluateBudget(user, it) }
    }

    override fun maybeRaiseLargeTransactionAlert(
        user: UserDTO,
        transactionId: UUID,
        amount: BigDecimal,
        description: String?,
    ) {
        evaluateLargeTransaction(user, users.fetchNotificationSettings(user.id), transactionId, amount, description)
    }

    override fun maybeRaiseLowBalanceAlerts(user: UserDTO, accountIds: Collection<UUID>?) {
        evaluateLowBalance(user, users.fetchNotificationSettings(user.id), accountIds)
    }

    override fun evaluateTransactionAlerts(
        user: UserDTO,
        transactionId: UUID,
        amount: BigDecimal,
        description: String?,
        accountId: UUID,
    ) {
        val settings = users.fetchNotificationSettings(user.id)
        evaluateLargeTransaction(user, settings, transactionId, amount, description)
        evaluateLowBalance(user, settings, listOf(accountId))
    }

    override fun runScheduledChecks(user: UserDTO) {
        val settings = users.fetchNotificationSettings(user.id)
        evaluateLoanDue(user, settings)
        evaluateRecurringUpcoming(user, settings)
        evaluateLowBalance(user, settings, null)
    }

    private fun evaluateLargeTransaction(
        user: UserDTO,
        settings: NotificationSettingsDTO,
        transactionId: UUID,
        amount: BigDecimal,
        description: String?,
    ) {
        val threshold = settings.largeTransactionThreshold ?: return
        if (threshold.signum() <= 0 || amount.abs() < threshold) return

        emitIfNew(
            user,
            NotificationType.LARGE_TRANSACTION,
            dedupKey = "tx:$transactionId",
            title = "Large transaction recorded",
            body = "A transaction of ${amount.abs().toPlainString()} was recorded${description?.let { ": $it" } ?: ""}.",
            extras = mapOf(
                "transactionId" to transactionId.toString(),
                "amount" to amount.toPlainString(),
            ),
        )
    }

    private fun evaluateLowBalance(
        user: UserDTO,
        settings: NotificationSettingsDTO,
        accountIds: Collection<UUID>?,
    ) {
        val threshold = settings.lowBalanceThreshold ?: return
        if (threshold.signum() <= 0) return

        // One alert per (account, week) so a balance hovering below the line doesn't spam the user.
        val weekKey = currentWeekKey()
        val all = accounts.fetchAllAccounts(user)
        val scoped = if (accountIds == null) all else all.filter { it.id in accountIds }
        for (account in scoped) {
            if (account.balance >= threshold) continue
            emitIfNew(
                user,
                NotificationType.LOW_ACCOUNT_BALANCE,
                dedupKey = "acct:${account.id}:$weekKey",
                title = "Low balance: ${account.name}",
                body = "${account.name} is at ${account.balance.toPlainString()} ${account.currency}, below your threshold of ${threshold.toPlainString()}.",
                extras = mapOf(
                    "accountId" to account.id.toString(),
                    "balance" to account.balance.toPlainString(),
                    "threshold" to threshold.toPlainString(),
                ),
            )
        }
    }

    private fun currentWeekKey(): String {
        val today = LocalDate.now()
        return "${YearMonth.from(today)}-W${(today.dayOfYear - 1) / 7 + 1}"
    }

    private fun evaluateLoanDue(user: UserDTO, settings: NotificationSettingsDTO) {
        val today = LocalDate.now()
        val openLoans = loans.fetchAllLoans(user).filter { it.outstanding.signum() > 0 && it.dueDate != null }

        for (loan in openLoans) {
            val due = loan.dueDate ?: continue
            val id = loan.id ?: continue
            val daysUntil = due.toEpochDay() - today.toEpochDay()
            val isOverdue = daysUntil < 0
            val isUpcoming = daysUntil in 0..settings.loanDueDaysAhead.toLong()
            if (!isOverdue && !isUpcoming) continue

            val contactName = loan.contact.name.takeIf { it.isNotBlank() } ?: "this contact"
            val outstanding = loan.outstanding.toPlainString()
            val (title, body) = if (isOverdue) {
                "Loan overdue from $contactName" to "A loan of $outstanding from $contactName was due on $due."
            } else {
                "Loan due soon from $contactName" to "A loan of $outstanding from $contactName is due on $due."
            }
            emitIfNew(
                user,
                NotificationType.LOAN_DUE,
                dedupKey = "loan:$id:${if (isOverdue) "overdue" else "due"}:$due",
                title = title,
                body = body,
                extras = mapOf(
                    "loanId" to id.toString(),
                    "contactId" to (loan.contact.id?.toString() ?: ""),
                    "dueDate" to due.toString(),
                    "outstanding" to outstanding,
                ),
            )
        }
    }

    private fun evaluateRecurringUpcoming(user: UserDTO, settings: NotificationSettingsDTO) {
        val today = LocalDate.now()
        val window = today.plusDays(settings.recurringDueDaysAhead.coerceAtLeast(0).toLong())
        val rules = recurring.fetchAll(user, null).filter { it.active && it.nextRunAt != null }

        for (rule in rules) {
            val next = rule.nextRunAt ?: continue
            val id = rule.id ?: continue
            if (next.isBefore(today) || next.isAfter(window)) continue
            emitIfNew(
                user,
                NotificationType.RECURRING_UPCOMING,
                dedupKey = "rec:$id:$next",
                title = "Recurring transaction upcoming",
                body = "${rule.description ?: "A recurring rule"} will run on $next.",
                extras = mapOf(
                    "recurringId" to id.toString(),
                    "runDate" to next.toString(),
                    "description" to (rule.description ?: ""),
                ),
            )
        }
    }

    /**
     * Inserts a notification iff no prior one shares the same [dedupKey] for this [user]+[type].
     * Always tags the payload with `dedupKey` so [INotificationRepository.existsByDedupKey] can find it.
     */
    private fun emitIfNew(
        user: UserDTO,
        type: NotificationType,
        dedupKey: String,
        title: String,
        body: String,
        extras: Map<String, String> = emptyMap(),
    ) {
        if (notifications.existsByDedupKey(user, type, dedupKey)) return
        notifications.create(user, type, title, body, mapOf("dedupKey" to dedupKey) + extras)
    }

    private fun evaluateBudget(user: UserDTO, b: BudgetDTO) {
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
