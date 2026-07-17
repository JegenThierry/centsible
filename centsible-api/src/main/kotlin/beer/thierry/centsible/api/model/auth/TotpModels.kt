package beer.thierry.centsible.api.model.auth

import jakarta.validation.constraints.NotBlank

/**
 * Returned when a user starts TOTP enrollment. [otpauthUri] is rendered as a QR client-side;
 * [secret] is shown as the manual-entry fallback. Both carry the same shared secret and must
 * only ever travel over HTTPS.
 */
data class TotpEnrollmentDTO(
    val otpauthUri: String,
    val secret: String,
)

/** The one-time recovery codes shown exactly once, right after enrollment is confirmed. */
data class RecoveryCodesDTO(
    val recoveryCodes: List<String>,
)

/** Current 2FA state for the signed-in user (drives the enroll-vs-disable UI). */
data class TotpStatusDTO(
    val enabled: Boolean,
    /** Count of unused recovery codes left (0 when 2FA is off), so the UI can nudge a regenerate. */
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
