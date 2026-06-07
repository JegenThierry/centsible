package beer.thierry.centsible.api.repository

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Persistence for the transient MFA artefacts that live outside the users row: the short-lived
 * pre-auth token bridging password-verify and code-submit, and the single-use recovery codes.
 */
interface IMfaRepository {
    /** Stores a hashed, time-boxed pre-auth token for [userId]. Returns the new row id. */
    fun createPendingAuth(userId: UUID, tokenHash: ByteArray, expiresAt: OffsetDateTime): UUID

    /**
     * Atomically consumes the pending-auth token whose stored hash equals [tokenHash], provided it
     * is unused and unexpired. Returns the owning user id on success, or null if no such live token
     * exists (already used, expired, or unknown). Marks the row used in the same statement so a
     * token cannot be redeemed twice.
     */
    fun consumePendingAuth(tokenHash: ByteArray): UUID?

    /** Removes any pending-auth rows for [userId] (e.g. on logout or a fresh login attempt). */
    fun deletePendingAuthForUser(userId: UUID): Int

    /** Replaces [userId]'s recovery codes with [codeHashes] (deletes the old set first). */
    fun replaceRecoveryCodes(userId: UUID, codeHashes: List<String>)

    /** Returns the hashes of [userId]'s still-unused recovery codes. */
    fun fetchUnusedRecoveryCodeHashes(userId: UUID): List<String>

    /** Marks the recovery code with the given [codeHash] used. Returns true if a row was updated. */
    fun markRecoveryCodeUsed(userId: UUID, codeHash: String): Boolean

    /** Deletes every recovery code for [userId] (when 2FA is disabled). */
    fun deleteRecoveryCodes(userId: UUID): Int
}
