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
    val data: Map<String, String> = emptyMap(),
    val readAt: OffsetDateTime? = null,
    val createdAt: OffsetDateTime,
)

data class NotificationSettingsDTO(
    val largeTransactionThreshold: BigDecimal? = null,
    val lowBalanceThreshold: BigDecimal? = null,
    val loanDueDaysAhead: Int = 3,
    val recurringDueDaysAhead: Int = 2,
    val budgetAlertsEnabled: Boolean = true,
)
