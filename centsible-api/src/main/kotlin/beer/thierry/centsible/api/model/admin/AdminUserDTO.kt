package beer.thierry.centsible.api.model.admin

import java.time.OffsetDateTime
import java.util.UUID

/** Per-user summary for the admin area: account metadata and coarse counts, never financial data. */
data class AdminUserDTO(
    val id: UUID = UUID.randomUUID(),
    val username: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val registered: Boolean = false,
    val totpEnabled: Boolean = false,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val lastLoginAt: OffsetDateTime? = null,
    val lastSeenAt: OffsetDateTime? = null,
    /** When the user last recorded a transaction (created_at, not the booking date). */
    val lastTransactionAt: OffsetDateTime? = null,
    val accountCount: Long = 0,
    val transactionCount: Long = 0,
)
