package beer.thierry.centsible.api.model.auth

import jakarta.validation.constraints.NotBlank

data class TotpEnrollmentDTO(
    val otpauthUri: String,
    val secret: String,
)

data class RecoveryCodesDTO(
    val recoveryCodes: List<String>,
)

data class TotpStatusDTO(
    val enabled: Boolean,
    val recoveryCodesRemaining: Int = 0,
)

/**
 * A 6-digit TOTP code (or a recovery code) submitted to confirm enrollment, to disable 2FA, or
 * during the second login step against the pre-auth token cookie.
 */
data class TotpCodeRequest(
    @field:NotBlank val code: String = "",
)

/**
 * Outcome of the first login step. Either the user is fully authenticated (no 2FA, real JWT in
 * [Authenticated.token]) or a second factor is required and a short-lived pre-auth token was
 * issued ([TwoFactorRequired.pendingToken]) for the challenge step.
 */
sealed interface LoginResult {
    data class Authenticated(val token: String) : LoginResult
    data class TwoFactorRequired(val pendingToken: String) : LoginResult
}
