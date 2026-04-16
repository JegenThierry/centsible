package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.auth.AuthRegisterRequest
import beer.thierry.budgetplanner.api.model.user.User
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.util.*

interface IUserRepository {
    fun findAllUsers(): List<UserDTO>
    fun findUserByUsername(username: String): User?
    fun findUserByEmail(email: String): User?
    fun findUserByEmailOrUsername(email: String, username: String): User?
    fun findUserById(id: UUID): User?
    fun findUserByTokenAndUsername(token: UUID, username: String): User?
    fun createUser(user: AuthRegisterRequest): User?
    fun confirmUser(id: UUID): Boolean
    fun updateUserProfile(id: UUID, firstName: String, lastName: String, email: String, profilePicture: String?): User?
}
