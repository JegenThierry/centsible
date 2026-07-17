package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.integrations.ProviderConnectionStatus
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Persistence contract for provider_connections. The plaintext shape used here is intentionally
 * generic so adding a new provider does not require schema changes.
 */
interface IProviderConnectionsRepository {

    fun fetchAll(authenticatedUser: UserDTO): List<ProviderConnectionRecord>
    fun fetchById(authenticatedUser: UserDTO, id: UUID): ProviderConnectionRecord?

    /** Returns the encrypted credentials blob for an update merge. Scoped to the user. */
    fun fetchEncryptedCredentialsById(authenticatedUser: UserDTO, id: UUID): ByteArray?

    fun create(
        userId: UUID,
        providerKey: String,
        displayName: String,
        status: ProviderConnectionStatus,
        config: Map<String, Any?>,
        credentials: ByteArray?,
    ): ProviderConnectionRecord

    fun update(
        authenticatedUser: UserDTO,
        id: UUID,
        displayName: String,
        status: ProviderConnectionStatus,
        config: Map<String, Any?>,
        credentials: ByteArray?,
    ): ProviderConnectionRecord?

    fun delete(authenticatedUser: UserDTO, id: UUID): Boolean

    /** Clears the sync schedule/error so [claimNextDueForSync] picks the connection up next poll; true if a row was updated. */
    fun requeueForSync(authenticatedUser: UserDTO, id: UUID): Boolean

    /** Worker-lease claim — atomic SELECT-FOR-UPDATE + UPDATE. Returns null if none due. */
    fun claimNextDueForSync(
        workerId: String,
        leaseTimeoutSeconds: Long,
        syncIntervalSeconds: Long,
    ): ProviderSyncCandidate?

    /** Releases the worker lease, clears the error, and stores [cursor] for the next incremental sync. */
    fun markSyncSuccess(id: UUID, cursor: String?)

    /** Releases the worker lease and records [errorMessage]; leaves the connection due so it retries. */
    fun markSyncError(id: UUID, errorMessage: String)
}

data class ProviderConnectionRecord(
    val id: UUID,
    val providerKey: String,
    val displayName: String,
    val status: ProviderConnectionStatus,
    val config: Map<String, Any?>,
    val lastSyncAt: OffsetDateTime?,
    val lastError: String?,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime,
)

data class ProviderSyncCandidate(
    val connectionId: UUID,
    val userId: UUID,
    val providerKey: String,
    val displayName: String,
    val config: Map<String, Any?>,
    val encryptedCredentials: ByteArray?,
    val lastSyncCursor: String?,
)
