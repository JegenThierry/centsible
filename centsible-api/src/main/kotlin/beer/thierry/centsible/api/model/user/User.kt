package beer.thierry.centsible.api.model.user

import java.util.*

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
    val createdAt: java.time.OffsetDateTime = java.time.OffsetDateTime.now(),
    val modifiedAt: java.time.OffsetDateTime = java.time.OffsetDateTime.now()
)
