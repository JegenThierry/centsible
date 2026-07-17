package beer.thierry.centsible.core.services.notifications

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.integrations.ProviderConnectionStatus
import beer.thierry.centsible.api.model.notification.NotificationDTO
import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
import beer.thierry.centsible.api.model.notification.NotificationType
import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.repository.ILoansRepository
import beer.thierry.centsible.api.repository.INotificationRepository
import beer.thierry.centsible.api.repository.IProviderConnectionsRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.api.services.notifications.INotificationService
import beer.thierry.centsible.core.services.recurring.occurrenceDatesInWindow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

private val THRESHOLD = BigDecimal("0.85")
private val EXCEEDED = BigDecimal("1.00")
private const val PACE_MIN_DAYS = 4

/** How many days before a provider consent lapses we start warning the user. */
private const val CONSENT_EXPIRY_LEAD_DAYS = 7L

/** How far ahead we project each account's recurring cash flow when warning about an upcoming shortfall. */
private const val PROJECTED_SHORTFALL_HORIZON_DAYS = 30L

/** Safety cap on generated occurrences per rule while projecting (a daily rule over 30d is ~30). */
private const val MAX_SHORTFALL_OCCURRENCES = 200

@Service
class NotificationService(
    private val notifications: INotificationRepository,
    private val budgets: IBudgetRepository,
    private val users: IUserRepository,
    private val loans: ILoansRepository,
    private val recurring: IRecurringTransactionRepository,
    private val accounts: IBudgetAccountsRepository,
    private val connections: IProviderConnectionsRepository,
    private val currencyConversionService: ICurrencyConversionService,
) : INotificationService {

    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    override fun list(user: UserDTO, limit: Int): List<NotificationDTO> = notifications.list(user, limit)

    override fun countUnread(user: UserDTO): Int = notifications.countUnread(user)

    override fun markRead(id: UUID, user: UserDTO): Boolean = notifications.markRead(id, user)

    override fun markAllRead(user: UserDTO): Int = notifications.markAllRead(user)

    override fun delete(id: UUID, user: UserDTO): Boolean = notifications.delete(id, user)

    override fun maybeRaiseBudgetAlerts(user: UserDTO, categoryIds: Collection<Long>?) {
        if (!users.fetchNotificationSettings(user.id).budgetAlertsEnabled) return
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
        evaluateLowBalance(user, users.fetchNotificationSettings(user.id), accounts.fetchAllAccounts(user), accountIds)
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
        evaluateLowBalance(user, settings, accounts.fetchAllAccounts(user), listOf(accountId))
    }

    override fun runScheduledChecks(user: UserDTO) {
        val settings = users.fetchNotificationSettings(user.id)
        // Fetched once and threaded into every check that needs them, rather than each evaluator
        // re-reading the same user-scoped tables on the same sweep.
        val recurringRules = recurring.fetchAll(user)
        val userAccounts = accounts.fetchAllAccounts(user)
        evaluateLoanDue(user, settings)
        evaluateRecurringUpcoming(user, settings, recurringRules)
        evaluateLowBalance(user, settings, userAccounts, null)
        evaluateProjectedShortfall(user, settings, recurringRules, userAccounts)
        evaluateBudgetPacing(user, settings)
        evaluateConsentExpiry(user)
    }

    override fun maybeRaiseSyncFailure(user: UserDTO, connectionId: UUID, provider: String, error: String?) {
        emitIfNew(
            user,
            NotificationType.SYNC_FAILED,
            dedupKey = "sync-fail:$connectionId:${LocalDate.now()}",
            title = "Bank sync failed",
            body = "Automatic sync for your $provider connection failed${error?.let { ": $it" } ?: ""}. Centsible will keep retrying.",
            extras = mapOf(
                "connectionId" to connectionId.toString(),
                "provider" to provider,
                "error" to (error ?: ""),
            ),
        )
    }

    /** Warns before a bank/provider consent lapses so the user can re-authorize before syncs break. */
    private fun evaluateConsentExpiry(user: UserDTO) {
        val today = LocalDate.now()
        for (connection in connections.fetchAll(user)) {
            if (connection.status == ProviderConnectionStatus.REVOKED) continue
            val expiresAt = parseInstant(connection.config["consentExpiresAt"]) ?: continue
            val expiryDate = LocalDate.ofInstant(expiresAt, ZoneOffset.UTC)
            val daysUntil = ChronoUnit.DAYS.between(today, expiryDate)
            // Already-expired consents surface as sync failures instead; here we only warn ahead of time.
            if (daysUntil < 0 || daysUntil > CONSENT_EXPIRY_LEAD_DAYS) continue
            emitIfNew(
                user,
                NotificationType.CONSENT_EXPIRING,
                dedupKey = "consent:${connection.id}:$expiryDate",
                title = "Bank connection expiring soon",
                body = "Access for your ${connection.displayName} connection expires on $expiryDate. Reconnect it to keep syncing.",
                extras = mapOf(
                    "connectionId" to connection.id.toString(),
                    "provider" to connection.providerKey,
                    "expiresAt" to expiryDate.toString(),
                ),
            )
        }
    }

    private fun parseInstant(raw: Any?): Instant? {
        val text = raw as? String ?: return null
        return runCatching { Instant.parse(text) }.getOrNull()
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
        userAccounts: List<BudgetAccountDTO>,
        accountIds: Collection<UUID>?,
    ) {
        val threshold = settings.lowBalanceThreshold ?: return
        if (threshold.signum() <= 0) return

        val weekKey = currentWeekKey()
        val scoped = if (accountIds == null) userAccounts else userAccounts.filter { it.id in accountIds }
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
            val amount = "$outstanding ${loan.currency}"
            val interestSuffix = loan.interestRate?.let { " (at ${it.toPlainString()}% interest)" } ?: ""
            val (title, body) = if (isOverdue) {
                "Loan overdue from $contactName" to "A loan of $amount from $contactName was due on $due$interestSuffix."
            } else {
                "Loan due soon from $contactName" to "A loan of $amount from $contactName is due on $due$interestSuffix."
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
                    "currency" to loan.currency,
                    "interestRate" to (loan.interestRate?.toPlainString() ?: ""),
                ),
            )
        }
    }

    private fun evaluateRecurringUpcoming(
        user: UserDTO,
        settings: NotificationSettingsDTO,
        rules: List<RecurringTransactionDTO>,
    ) {
        val today = LocalDate.now()
        val window = today.plusDays(settings.recurringDueDaysAhead.coerceAtLeast(0).toLong())
        val activeRules = rules.filter { it.active && it.nextRunAt != null }

        for (rule in activeRules) {
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
     * Proactive overdraft early-warning. Projects each account's balance forward over
     * [PROJECTED_SHORTFALL_HORIZON_DAYS] using its active recurring rules (income/expense on the
     * account plus transfers in/out) and alerts the first time it is projected to dip below the
     * user's [NotificationSettingsDTO.lowBalanceThreshold]. Accounts already under the threshold are
     * left to the reactive [evaluateLowBalance] alert. Amounts are already in each account's own
     * currency, except a transfer's destination credit — see [deltaForAccount].
     */
    private fun evaluateProjectedShortfall(
        user: UserDTO,
        settings: NotificationSettingsDTO,
        rules: List<RecurringTransactionDTO>,
        userAccounts: List<BudgetAccountDTO>,
    ) {
        val threshold = settings.lowBalanceThreshold ?: return
        if (threshold.signum() <= 0) return

        val activeRules = rules.filter { it.active }
        if (activeRules.isEmpty()) return

        val today = LocalDate.now()
        val horizonEnd = today.plusDays(PROJECTED_SHORTFALL_HORIZON_DAYS)
        // Occurrence dates depend only on the rule and the shared window, not the account, so walk
        // each rule's calendar once instead of re-walking it for every account.
        val datesByRule = activeRules.associateWith {
            occurrenceDatesInWindow(it, today, horizonEnd, MAX_SHORTFALL_OCCURRENCES)
        }
        val currencyByAccount = userAccounts.associate { it.id to it.currency }

        for (account in userAccounts) {
            // Already below the line — the reactive low-balance alert owns this case.
            if (account.balance < threshold) continue

            val impacts = activeRules.asSequence()
                .flatMap { rule ->
                    val delta = deltaForAccount(rule, account.id, currencyByAccount)
                        ?: return@flatMap emptySequence()
                    datesByRule.getValue(rule).asSequence().map { it to delta }
                }
                .sortedBy { it.first }

            var running = account.balance
            val (breachDate, breachBalance) = impacts.firstNotNullOfOrNull { (date, delta) ->
                running = running.add(delta)
                (date to running).takeIf { running < threshold }
            } ?: continue
            val atBreach = breachBalance.setScale(2, RoundingMode.HALF_UP)

            emitIfNew(
                user,
                NotificationType.PROJECTED_SHORTFALL,
                dedupKey = "shortfall:${account.id}:$breachDate",
                title = "Projected low balance: ${account.name}",
                body = "${account.name} is projected to fall to ${atBreach.toPlainString()} ${account.currency} on $breachDate, below your ${threshold.toPlainString()} ${account.currency} threshold.",
                extras = mapOf(
                    "accountId" to account.id.toString(),
                    "breachDate" to breachDate.toString(),
                    "projectedBalance" to atBreach.toPlainString(),
                    "threshold" to threshold.toPlainString(),
                    "currency" to account.currency.toString(),
                ),
            )
        }
    }

    /** Signed impact of one occurrence of [rule] on [accountId], or null if the rule doesn't touch it. */
    private fun deltaForAccount(
        rule: RecurringTransactionDTO,
        accountId: UUID,
        currencyByAccount: Map<UUID, Currency>,
    ): BigDecimal? {
        val amount = rule.amount ?: return null
        return when {
            rule.isTransfer && rule.accountId == accountId -> amount.negate()
            rule.isTransfer && rule.destinationAccountId == accountId ->
                creditedToDestination(amount, currencyByAccount[rule.accountId], currencyByAccount[accountId])
            !rule.isTransfer && rule.accountId == accountId -> when (rule.type) {
                CategoryType.INCOME -> amount
                CategoryType.EXPENSE -> amount.negate()
                null -> null
            }
            else -> null
        }
    }

    /**
     * A transfer's amount is entered in the source account's currency (ADR-0015), so the destination
     * leg is credited with the converted amount — the same conversion the materializer applies per
     * occurrence. Rules whose FX can't be resolved drop out of the projection rather than crediting a
     * raw foreign amount.
     */
    private fun creditedToDestination(amount: BigDecimal, from: Currency?, to: Currency?): BigDecimal? {
        if (from == null || to == null) return null
        if (from == to) return amount
        return runCatching { currencyConversionService.convert(amount, from, to, LocalDate.now()).convertedAmount }
            .getOrElse {
                log.warn("Excluding recurring transfer from shortfall projection: FX {}->{} unavailable", from, to, it)
                null
            }
    }

    private fun evaluateBudgetPacing(user: UserDTO, settings: NotificationSettingsDTO) {
        if (!settings.budgetAlertsEnabled) return
        val now = YearMonth.now()
        val daysElapsed = LocalDate.now().dayOfMonth
        if (daysElapsed < PACE_MIN_DAYS) return
        val daysInMonth = now.lengthOfMonth()

        for (b in budgets.fetchAllWithSpentForMonth(user, now)) {
            if (b.amountSpent.signum() <= 0) continue
            val view = decodeBudget(b) ?: continue
            if (view.ratio >= THRESHOLD) continue

            val projected = b.amountSpent
                .multiply(BigDecimal(daysInMonth))
                .divide(BigDecimal(daysElapsed), 2, RoundingMode.HALF_UP)
            if (projected <= view.limit) continue

            raise(
                user, NotificationType.BUDGET_PACE, view.budgetId, view.periodKey,
                title = "On track to exceed: ${view.category}",
                body = "At your current pace you'll spend about ${projected.toPlainString()} on ${view.category} this month, over your ${view.limit.toPlainString()} budget.",
                extras = mapOf(
                    "categoryName" to view.category,
                    "projected" to projected.toPlainString(),
                    "limit" to view.limit.toPlainString(),
                ),
            )
        }
    }

    /** Derived budget fields the pacing and threshold/exceeded evaluators both read. */
    private data class BudgetView(
        val limit: BigDecimal,
        val ratio: BigDecimal,
        val budgetId: String,
        val periodKey: String,
        val category: String,
    )

    /** Shared budget decode; null when the budget has no usable limit, id or period. */
    private fun decodeBudget(b: BudgetDTO): BudgetView? {
        val limit = b.amountLimit + b.rolloverAmount
        if (limit.signum() <= 0) return null
        val budgetId = b.id?.toString() ?: return null
        val periodKey = b.period ?: return null
        return BudgetView(
            limit = limit,
            ratio = b.amountSpent.divide(limit, 4, RoundingMode.HALF_UP),
            budgetId = budgetId,
            periodKey = periodKey,
            category = b.category.name ?: "",
        )
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
        val view = decodeBudget(b) ?: return

        when {
            view.ratio >= EXCEEDED -> raise(
                user, NotificationType.BUDGET_EXCEEDED, view.budgetId, view.periodKey,
                title = "Budget exceeded: ${view.category}",
                body = "You've spent more than your ${view.category} budget for this period.",
                extras = mapOf("categoryName" to view.category, "spent" to b.amountSpent.toPlainString(), "limit" to view.limit.toPlainString()),
            )
            view.ratio >= THRESHOLD -> {
                val percent = view.ratio.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toPlainString()
                raise(
                    user, NotificationType.BUDGET_THRESHOLD, view.budgetId, view.periodKey,
                    title = "Approaching budget: ${view.category}",
                    body = "You're at $percent% of your ${view.category} budget for this period.",
                    extras = mapOf("categoryName" to view.category, "percent" to percent),
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
