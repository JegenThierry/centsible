package beer.thierry.budgetplanner.api.model.user

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
    val registrationToken: UUID? = null,
    val createdAt: java.time.OffsetDateTime = java.time.OffsetDateTime.now(),
    val modifiedAt: java.time.OffsetDateTime = java.time.OffsetDateTime.now()
)
