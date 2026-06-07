package beer.thierry.centsible.api.services.authentication

import beer.thierry.centsible.api.model.auth.RecoveryCodesDTO
import beer.thierry.centsible.api.model.auth.TotpEnrollmentDTO
import beer.thierry.centsible.api.model.auth.TotpStatusDTO
import java.util.UUID

/**
 * TOTP (RFC 6238) two-factor enrollment and verification. Authorization is the caller's
 * responsibility for the login-challenge path (which runs pre-authentication); the enroll/
 * confirm/disable/status methods act on the authenticated user's own id.
 */
interface ITotpService {
    /** Current 2FA state for [userId]. */
    fun status(userId: UUID): TotpStatusDTO

    /**
     * Starts enrollment: generates a fresh secret, stores it encrypted in the pending slot, and
     * returns the otpauth URI + plaintext secret. Does NOT enable 2FA — that happens at confirm.
     * Re-calling overwrites any prior unconfirmed pending secret.
     */
    fun beginEnrollment(userId: UUID, accountName: String): TotpEnrollmentDTO

    /**
     * Confirms enrollment by verifying [code] against the pending secret. On success, promotes the
     * secret to active, enables 2FA, generates the recovery codes and returns them (shown once).
     * Throws if there is no pending secret or the code is invalid.
     */
    fun confirmEnrollment(userId: UUID, code: String): RecoveryCodesDTO

    /**
     * Disables 2FA after verifying [code] (a current TOTP code or an unused recovery code). Clears
     * the secret and recovery codes. Throws if 2FA isn't enabled or the code is invalid.
     */
    fun disable(userId: UUID, code: String)

    /** Cancels an in-progress enrollment by discarding the pending secret. No-op if none is pending. */
    fun cancelEnrollment(userId: UUID)

    /**
     * Verifies a login-challenge [code] (TOTP or recovery) against [userId]'s active secret,
     * enforcing replay rejection. Returns true on success. Used by the auth service's challenge step.
     */
    fun verifyChallengeCode(userId: UUID, code: String): Boolean
}
