package beer.thierry.budgetplanner.api.services.authentication

import beer.thierry.budgetplanner.api.model.auth.AuthRegisterRequest
import beer.thierry.budgetplanner.api.model.auth.AuthRequest
import beer.thierry.budgetplanner.api.model.auth.AuthResponse

interface IAuthService {
    fun authenticate(authRequest: AuthRequest): AuthResponse
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
}
