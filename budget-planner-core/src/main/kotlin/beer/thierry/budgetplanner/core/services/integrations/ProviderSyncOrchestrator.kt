package beer.thierry.budgetplanner.core.services.integrations

import beer.thierry.budgetplanner.api.model.integrations.ProviderContext
import beer.thierry.budgetplanner.api.repository.IProviderConnectionsRepository
import beer.thierry.budgetplanner.api.repository.ProviderSyncCandidate
import beer.thierry.budgetplanner.api.services.integrations.IAccountProvider
import beer.thierry.budgetplanner.api.services.integrations.IProviderRegistry
import beer.thierry.budgetplanner.api.services.integrations.ITransactionImporter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Polls for ACTIVE connections that are due for sync, dispatches each to the matching provider,
 * and updates the connection's sync metadata. Uses the same worker-claim pattern as the export
 * service so multiple rest instances can run safely.
 */
@Component
class ProviderSyncOrchestrator(
    private val registry: IProviderRegistry,
    private val repository: IProviderConnectionsRepository,
    private val cipher: CredentialCipher,
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
        val ctx = ProviderContext(
            userId = candidate.userId,
            connectionId = candidate.connectionId,
            displayName = candidate.displayName,
            config = candidate.config,
            credentials = if (needsCredentials) cipher.decrypt(candidate.encryptedCredentials) else emptyMap(),
            lastCursor = candidate.lastSyncCursor,
        )

        if (module is IAccountProvider) {
            val accounts = module.listExternalAccounts(ctx)
            log.info(
                "Provider {} reported {} external account(s) for connection {}",
                candidate.providerKey, accounts.size, candidate.connectionId,
            )
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
}
