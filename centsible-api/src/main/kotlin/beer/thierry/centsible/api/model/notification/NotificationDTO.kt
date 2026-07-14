package beer.thierry.centsible.api.model.notification

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

enum class NotificationType {
    BUDGET_THRESHOLD,
    BUDGET_EXCEEDED,
    BUDGET_PACE,
    LOAN_DUE,
    RECURRING_UPCOMING,
    LARGE_TRANSACTION,
    LOW_ACCOUNT_BALANCE,
    PROJECTED_SHORTFALL,
    SYNC_FAILED,
    CONSENT_EXPIRING,
}

data class NotificationDTO(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val body: String,
    /** Structured payload whose keys depend on [type] (e.g. the related account or budget id). */
    val data: Map<String, String> = emptyMap(),
    val readAt: OffsetDateTime? = null,
    val createdAt: OffsetDateTime,
)

/**
 * Per-user notification thresholds. Null values mean the alert is disabled.
 */
data class NotificationSettingsDTO(
    val largeTransactionThreshold: BigDecimal? = null,
    val lowBalanceThreshold: BigDecimal? = null,
    val loanDueDaysAhead: Int = 3,
    val recurringDueDaysAhead: Int = 2,
    /** Master switch for the category-budget alerts (threshold / exceeded / pace). Defaults on. */
    val budgetAlertsEnabled: Boolean = true,
)
