package beer.thierry.budgetplannerrest.model.user

import java.util.UUID

data class UserDTO (
    val id: UUID,
    val username: String,
    val email: String,
    val name: String,
    val image: String
)