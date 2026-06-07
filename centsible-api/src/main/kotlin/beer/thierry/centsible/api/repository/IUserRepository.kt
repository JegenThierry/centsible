package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.auth.AuthRegisterRequest
import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
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

    /** Returns all user IDs — used by the notification scheduler to sweep per-user checks. */
    fun fetchAllUserIds(): List<UUID>

    /** Reads the per-user notification settings JSON (or returns defaults if unset). */
    fun fetchNotificationSettings(id: UUID): NotificationSettingsDTO

    fun updateNotificationSettings(id: UUID, settings: NotificationSettingsDTO): NotificationSettingsDTO

    // --- TOTP two-factor (opt-in) ---

    /** Stores the encrypted secret in the pending slot during enrollment (before the user confirms). */
    fun savePendingTotpSecret(id: UUID, encryptedSecret: ByteArray): Boolean

    /** Reads the encrypted pending secret captured at enrollment start (null once promoted/cleared). */
    fun getPendingTotpSecret(id: UUID): ByteArray?

    /** Reads the encrypted active secret (null when 2FA is not enabled). */
    fun getActiveTotpSecret(id: UUID): ByteArray?

    /** Promotes the pending secret to active, flips totp_enabled on, and clears the pending slot. */
    fun activateTotp(id: UUID, encryptedSecret: ByteArray): Boolean

    /** Turns 2FA off and clears every TOTP column (secret, pending, last-used step, enabled_at). */
    fun disableTotp(id: UUID): Boolean

    /** Last successfully consumed 30s time-step, for replay rejection (null if none yet). */
    fun getTotpLastUsedStep(id: UUID): Long?

    /** Records the time-step of a successful verification so the same window cannot be replayed. */
    fun updateTotpLastUsedStep(id: UUID, step: Long): Boolean
}
