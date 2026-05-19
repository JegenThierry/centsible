package beer.thierry.centsible.api.model.notification

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

enum class NotificationType {
    BUDGET_THRESHOLD,
    BUDGET_EXCEEDED,
    LOAN_DUE,
    RECURRING_UPCOMING,
    LARGE_TRANSACTION,
    LOW_ACCOUNT_BALANCE,
}

data class NotificationDTO(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val body: String,
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
)
