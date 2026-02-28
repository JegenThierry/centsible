package beer.thierry.budgetplannerrest.repository.users

import beer.thierry.budgetplannerrest.model.auth.AuthRegisterRequest
import beer.thierry.budgetplannerrest.model.user.User
import beer.thierry.budgetplannerrest.model.user.UserDTO
import java.util.UUID

interface IUserRepository {
    fun findAllUsers(): List<UserDTO>
    fun findUserByUsername(username: String): User?
    fun findUserById(id: UUID): User?
    fun createUser(user: AuthRegisterRequest): User?
}