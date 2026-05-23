package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.integrations.OAuthCredentialEnvelope
import beer.thierry.centsible.api.model.integrations.ProviderConnectionStatus
import beer.thierry.centsible.api.model.integrations.ProviderContext
import beer.thierry.centsible.api.repository.IProviderConnectionsRepository
import beer.thierry.centsible.api.services.integrations.IOAuthFlowProvider
import beer.thierry.centsible.api.services.integrations.ProviderModule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Wraps a provider sync invocation with proactive OAuth token refresh.
 *
 * The orchestrator calls [withFreshTokens] before each capability call. If the credentials
 * contain an OAuth envelope nearing expiry (and the provider implements IOAuthFlowProvider),
 * we ask the provider for a fresh envelope, persist it atomically, and return a context that
 * reflects the new tokens. Providers without OAuth pass through unchanged.
 */
@Component
class TokenRefreshGuard(
    private val repository: IProviderConnectionsRepository,
    private val cipher: CredentialCipher,
) {
    private val log = LoggerFactory.getLogger(TokenRefreshGuard::class.java)

    fun withFreshTokens(
        module: ProviderModule,
        ctx: ProviderContext,
    ): ProviderContext {
        if (module !is IOAuthFlowProvider) return ctx
        val current = OAuthCredentialEnvelope.from(ctx.credentials) ?: return ctx
        if (!current.isExpired(SAFETY_WINDOW_SECONDS)) return ctx

        log.info("Refreshing OAuth token for connection {} (provider={})",
            ctx.connectionId, module.descriptor.key)
        val refreshed = try {
            module.refreshAccessToken(ctx)
        } catch (e: Exception) {
            log.warn("Token refresh failed for connection {}; marking REVOKED", ctx.connectionId, e)
            persistRevoked(ctx.userId, ctx.connectionId, "Token refresh failed: ${e.message ?: e.javaClass.simpleName}")
            throw e
        }
        val rotatedCredentials = ctx.credentials.mergedWith(refreshed.toMap())
        persistRotatedCredentials(ctx.userId, ctx.connectionId, rotatedCredentials, ctx.config)
        return ctx.copy(credentials = rotatedCredentials)
    }

    private fun persistRotatedCredentials(
        userId: UUID,
        connectionId: UUID,
        credentials: Map<String, String>,
        config: Map<String, Any?>,
    ) {
        val user = syntheticUser(userId)
        val existing = repository.fetchById(user, connectionId) ?: run {
            log.warn("Cannot persist refreshed token: connection {} not found", connectionId)
            return
        }
        repository.update(
            authenticatedUser = user,
            id = connectionId,
            displayName = existing.displayName,
            status = existing.status,
            config = config,
            credentials = cipher.encrypt(credentials),
        )
    }

    private fun persistRevoked(userId: UUID, connectionId: UUID, reason: String) {
        val user = syntheticUser(userId)
        val existing = repository.fetchById(user, connectionId) ?: return
        repository.update(
            authenticatedUser = user,
            id = connectionId,
            displayName = existing.displayName,
            status = ProviderConnectionStatus.REVOKED,
            config = existing.config,
            credentials = repository.fetchEncryptedCredentialsById(user, connectionId),
        )
        repository.markSyncError(connectionId, reason)
    }

    private companion object {
        const val SAFETY_WINDOW_SECONDS = 60L
    }
}
