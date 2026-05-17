package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.auth.AuthRegisterRequest
import beer.thierry.centsible.api.model.user.User
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

    /** Stores a hashed password reset token + expiry on the user, replacing any existing one. */
    fun setPasswordResetToken(id: UUID, tokenHash: ByteArray, expiresAt: OffsetDateTime): Boolean

    /** Returns the user whose stored password reset hash equals [tokenHash] AND whose token has not yet expired. */
    fun findUserByValidPasswordResetTokenHash(tokenHash: ByteArray): User?

    /** Sets a new password hash and clears the reset token in one atomic update. */
    fun resetPassword(id: UUID, newPasswordHash: String): Boolean
}
