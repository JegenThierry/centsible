package beer.thierry.centsible.api.model.admin

import java.time.OffsetDateTime
import java.util.UUID

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
    val lastTransactionAt: OffsetDateTime? = null,
    val accountCount: Long = 0,
    val transactionCount: Long = 0,
)
