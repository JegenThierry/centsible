package beer.thierry.centsible.api.model.user

import java.time.OffsetDateTime
import java.util.UUID

data class User(
    val id: UUID = UUID.randomUUID(),
    val username: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val passwordHash: String = "",
    val profilePicture: String? = null,
    val registered: Boolean = false,
    val locale: String = "en",
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val modifiedAt: OffsetDateTime = OffsetDateTime.now(),
)

