package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.auth.AuthRegisterRequest
import beer.thierry.budgetplanner.api.model.user.User
import java.time.OffsetDateTime
import java.util.*

interface IUserRepository {
    fun findUserByUsername(username: String): User?
    fun findUserByEmail(email: String): User?
    fun findUserByEmailOrUsername(email: String, username: String): User?
    fun findUserById(id: UUID): User?
    /** Returns the user whose stored hash equals [tokenHash] AND whose token has not yet expired. */
    fun findUserByValidTokenHash(tokenHash: ByteArray): User?
    fun createUser(
        user: AuthRegisterRequest,
        passwordHash: String,
        registrationTokenHash: ByteArray,
        registrationTokenExpiresAt: OffsetDateTime,
    ): User?
    fun confirmUser(id: UUID): Boolean
    fun updateUserProfile(id: UUID, firstName: String, lastName: String, email: String, profilePicture: String?): User?
    fun updateUserLocale(id: UUID, locale: String): User?
}
