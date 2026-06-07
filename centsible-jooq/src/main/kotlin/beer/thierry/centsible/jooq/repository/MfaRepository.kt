package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.repository.IMfaRepository
import beer.thierry.jooq.generated.tables.references.MFA_PENDING_AUTH
import beer.thierry.jooq.generated.tables.references.USER_RECOVERY_CODES
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class MfaRepository(
    private val dsl: DSLContext,
) : IMfaRepository {

    override fun createPendingAuth(userId: UUID, tokenHash: ByteArray, expiresAt: OffsetDateTime): UUID =
        dsl.insertInto(MFA_PENDING_AUTH)
            .set(MFA_PENDING_AUTH.USER_ID, userId)
            .set(MFA_PENDING_AUTH.TOKEN_HASH, tokenHash)
            .set(MFA_PENDING_AUTH.EXPIRES_AT, expiresAt)
            .set(MFA_PENDING_AUTH.CREATED_AT, OffsetDateTime.now())
            .returningResult(MFA_PENDING_AUTH.ID)
            .fetchOne(MFA_PENDING_AUTH.ID)
            ?: error("failed to insert pending auth row")

    override fun consumePendingAuth(tokenHash: ByteArray): UUID? =
        // Single-statement claim: only flips used_at on a row that is still unused and unexpired,
        // returning its user id. A second redemption of the same token matches nothing.
        dsl.update(MFA_PENDING_AUTH)
            .set(MFA_PENDING_AUTH.USED_AT, OffsetDateTime.now())
            .where(MFA_PENDING_AUTH.TOKEN_HASH.eq(tokenHash))
            .and(MFA_PENDING_AUTH.USED_AT.isNull)
            .and(MFA_PENDING_AUTH.EXPIRES_AT.gt(OffsetDateTime.now()))
            .returningResult(MFA_PENDING_AUTH.USER_ID)
            .fetchOne(MFA_PENDING_AUTH.USER_ID)

    override fun deletePendingAuthForUser(userId: UUID): Int =
        dsl.deleteFrom(MFA_PENDING_AUTH)
            .where(MFA_PENDING_AUTH.USER_ID.eq(userId))
            .execute()

    override fun replaceRecoveryCodes(userId: UUID, codeHashes: List<String>) {
        dsl.transaction { config ->
            val tx = config.dsl()
            tx.deleteFrom(USER_RECOVERY_CODES)
                .where(USER_RECOVERY_CODES.USER_ID.eq(userId))
                .execute()
            codeHashes.forEach { hash ->
                tx.insertInto(USER_RECOVERY_CODES)
                    .set(USER_RECOVERY_CODES.USER_ID, userId)
                    .set(USER_RECOVERY_CODES.CODE_HASH, hash)
                    .set(USER_RECOVERY_CODES.CREATED_AT, OffsetDateTime.now())
                    .execute()
            }
        }
    }

    override fun fetchUnusedRecoveryCodeHashes(userId: UUID): List<String> =
        dsl.select(USER_RECOVERY_CODES.CODE_HASH)
            .from(USER_RECOVERY_CODES)
            .where(USER_RECOVERY_CODES.USER_ID.eq(userId))
            .and(USER_RECOVERY_CODES.USED_AT.isNull)
            .fetch { it[USER_RECOVERY_CODES.CODE_HASH]!! }

    override fun markRecoveryCodeUsed(userId: UUID, codeHash: String): Boolean =
        dsl.update(USER_RECOVERY_CODES)
            .set(USER_RECOVERY_CODES.USED_AT, OffsetDateTime.now())
            .where(USER_RECOVERY_CODES.USER_ID.eq(userId))
            .and(USER_RECOVERY_CODES.CODE_HASH.eq(codeHash))
            .and(USER_RECOVERY_CODES.USED_AT.isNull)
            .execute() > 0

    override fun deleteRecoveryCodes(userId: UUID): Int =
        dsl.deleteFrom(USER_RECOVERY_CODES)
            .where(USER_RECOVERY_CODES.USER_ID.eq(userId))
            .execute()
}
