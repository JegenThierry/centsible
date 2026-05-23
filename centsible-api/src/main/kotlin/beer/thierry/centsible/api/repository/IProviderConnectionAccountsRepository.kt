package beer.thierry.centsible.api.repository

import java.util.UUID

/**
 * Persistence contract for provider_connection_accounts — the join table mapping a provider
 * connection's external account ids (e.g. a GoCardless account UUID) to the centsible accounts
 * row that holds the imported balances. The mapping was previously a JSONB blob inside
 * provider_connections.config; promoting it to a table gives us FK integrity, ON DELETE
 * CASCADE on connection removal, and the ability to query "which centsible account owns this
 * external id" without a Jackson round-trip.
 */
interface IProviderConnectionAccountsRepository {

    fun fetchAllForConnection(providerConnectionId: UUID): List<ProviderConnectionAccountRecord>

    fun fetchByExternalId(providerConnectionId: UUID, externalAccountId: String): ProviderConnectionAccountRecord?

    /**
     * Inserts a new mapping or updates an existing one matched on
     * (providerConnectionId, externalAccountId). Returns the persisted record.
     */
    fun upsert(
        providerConnectionId: UUID,
        externalAccountId: String,
        accountId: UUID?,
        externalMetadata: Map<String, Any?> = emptyMap(),
    ): ProviderConnectionAccountRecord
}

data class ProviderConnectionAccountRecord(
    val id: UUID,
    val providerConnectionId: UUID,
    val accountId: UUID?,
    val externalAccountId: String,
    val externalMetadata: Map<String, Any?>,
)
