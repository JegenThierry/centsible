package beer.thierry.budgetplannerrest.model.user

import java.util.UUID

data class User(
    val id: UUID,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val passwordHash: String,
    val createdAt: java.time.OffsetDateTime,
    val modifiedAt: java.time.OffsetDateTime
)
