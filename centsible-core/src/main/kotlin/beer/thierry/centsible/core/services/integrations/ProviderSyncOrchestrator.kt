package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.integrations.ExternalAccountDTO
import beer.thierry.centsible.api.model.integrations.ProviderContext
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IProviderConnectionAccountsRepository
import beer.thierry.centsible.api.repository.IProviderConnectionsRepository
import beer.thierry.centsible.api.repository.ProviderSyncCandidate
import beer.thierry.centsible.api.services.integrations.IAccountProvider
import beer.thierry.centsible.api.services.integrations.IProviderRegistry
import beer.thierry.centsible.api.services.integrations.ITransactionImporter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Polls for ACTIVE connections that are due for sync, dispatches each to the matching provider,
 * and updates the connection's sync metadata. Uses the same worker-claim pattern as the export
 * service so multiple rest instances can run safely.
 *
 * Per-connection behavior:
 *  1. Run OAuth token refresh via TokenRefreshGuard if the envelope is close to expiring.
 *  2. If the provider implements IAccountProvider, list external accounts and ensure each has
 *     a corresponding centsible accounts row, recording the mapping in provider_connection_accounts.
 *     Auto-create centsible accounts for external ids we have not seen before.
 *  3. If the provider implements ITransactionImporter, fetch new transactions since the last
 *     cursor. (Transaction persistence is delegated to the existing transactions pipeline in
 *     a follow-up; for v1 we log the count.)
 */
@Component
class ProviderSyncOrchestrator(
    private val registry: IProviderRegistry,
    private val repository: IProviderConnectionsRepository,
    private val cipher: CredentialCipher,
    private val tokenRefreshGuard: TokenRefreshGuard,
    private val accountsRepository: IBudgetAccountsRepository,
    private val connectionAccountsRepository: IProviderConnectionAccountsRepository,
    @Value("\${integrations.sync.lease-timeout-seconds:300}") private val leaseTimeoutSeconds: Long,
    @Value("\${integrations.sync.interval-seconds:3600}") private val syncIntervalSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(ProviderSyncOrchestrator::class.java)
    private val workerId = "rest-${UUID.randomUUID().toString().take(8)}"

    @Scheduled(
        fixedDelayString = "\${integrations.sync.poll-interval-ms:300000}",
        initialDelayString = "\${integrations.sync.initial-delay-ms:30000}",
    )
    fun pollOnce() {
        try {
            val candidate = repository.claimNextDueForSync(workerId, leaseTimeoutSeconds, syncIntervalSeconds)
                ?: return
            try {
                runSync(candidate)
            } catch (e: Exception) {
                log.warn("Sync failed for connection {} (provider={})", candidate.connectionId, candidate.providerKey, e)
                repository.markSyncError(candidate.connectionId, e.message ?: e.javaClass.simpleName)
            }
        } catch (e: Exception) {
            log.error("Sync poller crashed; will retry on next tick", e)
        }
    }

    private fun runSync(candidate: ProviderSyncCandidate) {
        val module = registry.getModule(candidate.providerKey)
        if (module == null) {
            log.warn(
                "No registered provider for key '{}'; marking connection {} as ERROR",
                candidate.providerKey, candidate.connectionId,
            )
            repository.markSyncError(
                candidate.connectionId,
                "Provider '${candidate.providerKey}' is no longer registered on this server",
            )
            return
        }
        val needsCredentials = module is IAccountProvider || module is ITransactionImporter
        var ctx = ProviderContext(
            userId = candidate.userId,
            connectionId = candidate.connectionId,
            displayName = candidate.displayName,
            config = candidate.config,
            credentials = if (needsCredentials) cipher.decrypt(candidate.encryptedCredentials) else emptyMap(),
            lastCursor = candidate.lastSyncCursor,
        )
        ctx = tokenRefreshGuard.withFreshTokens(module, ctx)

        if (module is IAccountProvider) {
            val externalAccounts = module.listExternalAccounts(ctx)
            log.info(
                "Provider {} reported {} external account(s) for connection {}",
                candidate.providerKey, externalAccounts.size, candidate.connectionId,
            )
            reconcileAccounts(candidate, externalAccounts)
        }

        val nextCursor = if (module is ITransactionImporter) {
            val page = module.importSince(ctx, candidate.lastSyncCursor)
            log.info(
                "Provider {} returned {} transaction(s) (nextCursor={}) for connection {}",
                candidate.providerKey, page.transactions.size, page.nextCursor, candidate.connectionId,
            )
            page.nextCursor ?: candidate.lastSyncCursor
        } else {
            candidate.lastSyncCursor
        }

        repository.markSyncSuccess(candidate.connectionId, nextCursor)
    }

    /**
     * Ensures each external account has a centsible accounts row AND a row in
     * provider_connection_accounts mapping the two. First sync auto-creates accounts;
     * subsequent syncs are no-ops for already-mapped external ids. New external accounts
     * showing up later (e.g. user opens a new sub-account at their bank) auto-register.
     */
    private fun reconcileAccounts(
        candidate: ProviderSyncCandidate,
        externalAccounts: List<ExternalAccountDTO>,
    ) {
        if (externalAccounts.isEmpty()) return
        val existing = connectionAccountsRepository
            .fetchAllForConnection(candidate.connectionId)
            .associateBy { it.externalAccountId }
        val user = syntheticUser(candidate.userId)
        for (account in externalAccounts) {
            if (existing.containsKey(account.externalId)) continue
            val created = accountsRepository.createAccount(
                user,
                CreateBudgetAccountRequest(
                    name = "${candidate.displayName}: ${account.name}".take(100),
                    initialBalance = account.balance ?: java.math.BigDecimal.ZERO,
                    currency = parseCurrency(account.currency),
                ),
            )
            connectionAccountsRepository.upsert(
                providerConnectionId = candidate.connectionId,
                externalAccountId = account.externalId,
                accountId = created.id,
                externalMetadata = buildMetadata(account),
            )
            log.info(
                "Auto-created centsible account {} for external account {} on connection {}",
                created.id, account.externalId, candidate.connectionId,
            )
        }
    }

    private fun buildMetadata(account: ExternalAccountDTO): Map<String, Any?> = buildMap {
        account.type?.let { put("type", it) }
        account.currency?.let { put("currency", it) }
        put("displayName", account.name)
    }

    private fun parseCurrency(code: String?): Currency =
        if (code.isNullOrBlank()) Currency.EUR
        else runCatching { Currency.valueOf(code.uppercase()) }.getOrElse { Currency.EUR }
}
