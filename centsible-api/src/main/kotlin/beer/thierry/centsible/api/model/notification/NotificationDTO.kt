package beer.thierry.centsible.api.model.notification

import java.time.OffsetDateTime
import java.util.*

enum class NotificationType {
    BUDGET_THRESHOLD,
    BUDGET_EXCEEDED,
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
