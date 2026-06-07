package beer.thierry.centsible.api.services.authentication

import beer.thierry.centsible.api.model.auth.AuthRegisterRequest
import beer.thierry.centsible.api.model.auth.AuthRequest
import beer.thierry.centsible.api.model.auth.AuthResponse
import beer.thierry.centsible.api.model.auth.LoginResult
import java.util.UUID

interface IAuthService {
    /**
     * First login step: verifies credentials. Returns [LoginResult.Authenticated] with a real JWT
     * when the account has no 2FA, or [LoginResult.TwoFactorRequired] with a short-lived pre-auth
     * token when a second factor is needed.
     */
    fun authenticate(authRequest: AuthRequest): LoginResult

    /**
     * Second login step: redeems the [pendingToken] (single-use, time-boxed) and verifies the TOTP
     * or recovery [code], returning the real JWT on success. Throws on an invalid/expired token or
     * a bad code.
     */
    fun completeTwoFactorChallenge(pendingToken: String, code: String): AuthResponse

    fun register(authRequest: AuthRegisterRequest): AuthResponse
    fun confirmRegistration(token: String): Boolean

    /**
     * Triggers a password reset for the account identified by [username]. Always succeeds
     * silently from the caller's perspective — unknown usernames must not be distinguishable
     * from known ones to avoid leaking account existence. If the account exists, a one-time
     * reset link is emailed.
     */
    fun requestPasswordReset(username: String)

    /**
     * Validates [token] and, on success, replaces the user's password with [newPassword].
     * Returns true on success, false when the token is invalid or expired.
     */
    fun resetPassword(token: String, newPassword: String): Boolean

    /**
     * Authenticated password change for [userId]. Verifies [currentPassword], enforces the new
     * password's strength rules, rejects reusing the current password, then re-encodes and stores
     * [newPassword]. Throws [LocalizedException.Unauthorized] when the current password is wrong.
     *
     * Bumps the user's token version, revoking every other live session, and returns a fresh JWT for
     * the caller so the current session stays signed in. The controller reissues the auth cookie with it.
     */
    fun changePassword(userId: UUID, currentPassword: String, newPassword: String): String

    /**
     * Revokes all of [userId]'s sessions by bumping the token version, then returns a fresh JWT for
     * the caller so the current device stays signed in. Every other device is signed out on its next
     * request. The controller reissues the auth cookie with the returned token.
     */
    fun signOutOtherSessions(userId: UUID): String

    /**
     * Irreversibly deletes [userId]'s account after verifying [password] (and, when 2FA is enabled,
     * a current [totpCode] — TOTP or recovery code). The delete cascades to all of the user's data
     * via database foreign keys. Throws [LocalizedException.Unauthorized] on a failed check.
     */
    fun deleteAccount(userId: UUID, password: String, totpCode: String?)
}
