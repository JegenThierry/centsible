package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.integrations.OAuthCallbackRequest
import beer.thierry.centsible.api.model.integrations.OAuthStartRequest
import beer.thierry.centsible.api.model.integrations.ProviderConnectionStatus
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IProviderConnectionsRepository
import beer.thierry.centsible.api.services.integrations.IOAuthFlowProvider
import beer.thierry.centsible.api.services.integrations.IOAuthFlowService
import beer.thierry.centsible.api.services.integrations.IProviderRegistry
import beer.thierry.centsible.api.services.integrations.OAuthCompletionResult
import beer.thierry.centsible.api.services.integrations.OAuthStartResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.util.UUID

@Service
class OAuthFlowService(
    private val registry: IProviderRegistry,
    private val repository: IProviderConnectionsRepository,
    private val cipher: CredentialCipher,
    private val stateStore: OAuthStateStore,
    @Value("\${integrations.base-url:}") private val baseUrl: String,
) : IOAuthFlowService {

    private val log = LoggerFactory.getLogger(OAuthFlowService::class.java)

    override fun startAuthorization(authenticatedUser: UserDTO, connectionId: UUID): OAuthStartResult {
        val record = repository.fetchById(authenticatedUser, connectionId)
            ?: throw IllegalArgumentException("Connection not found")
        val module = registry.getModule(record.providerKey)
            ?: throw IllegalStateException("Provider '${record.providerKey}' is no longer registered")
        require(module is IOAuthFlowProvider) {
            "Provider '${record.providerKey}' does not support the OAuth start flow"
        }
        val redirectUri = "${resolveBaseUrl()}/api/integrations/oauth/callback/${record.providerKey}"
        val state = stateStore.mint(authenticatedUser.id, connectionId, record.providerKey)

        val started = module.buildAuthorizationUrl(
            OAuthStartRequest(
                connectionId = connectionId,
                userId = authenticatedUser.id,
                displayName = record.displayName,
                config = record.config,
                redirectUri = redirectUri,
                state = state,
            )
        )
        if (started.configPatch.isNotEmpty()) {
            repository.update(
                authenticatedUser = authenticatedUser,
                id = connectionId,
                displayName = record.displayName,
                status = record.status,
                config = record.config + started.configPatch,
                credentials = repository.fetchEncryptedCredentialsById(authenticatedUser, connectionId),
            )
        }
        log.info("OAuth start: connection={} provider={}", connectionId, record.providerKey)
        return OAuthStartResult(authorizationUrl = started.authorizationUrl, connectionId = connectionId)
    }

    override fun completeAuthorization(
        providerKey: String,
        state: String,
        queryParams: Map<String, String>,
    ): OAuthCompletionResult {
        val stateRecord = stateStore.consume(state, providerKey)
            ?: return OAuthCompletionResult.Failure(null, ERROR_STATE_INVALID)

        val module = registry.getModule(stateRecord.providerKey)
        if (module !is IOAuthFlowProvider) {
            log.warn("OAuth callback: provider {} not registered or non-OAuth", stateRecord.providerKey)
            return OAuthCompletionResult.Failure(stateRecord.connectionId, ERROR_PROVIDER_GONE)
        }

        val callbackUser = syntheticUser(stateRecord.userId)
        val record = repository.fetchById(callbackUser, stateRecord.connectionId)
            ?: return OAuthCompletionResult.Failure(stateRecord.connectionId, ERROR_CONNECTION_GONE)
        if (record.providerKey != stateRecord.providerKey) {
            log.warn("OAuth callback: provider mismatch (state={}, record={})",
                stateRecord.providerKey, record.providerKey)
            return OAuthCompletionResult.Failure(stateRecord.connectionId, ERROR_PROVIDER_MISMATCH)
        }

        val redirectUri = "${resolveBaseUrl()}/api/integrations/oauth/callback/${record.providerKey}"
        val currentSecrets = cipher.decrypt(
            repository.fetchEncryptedCredentialsById(callbackUser, stateRecord.connectionId)
        )

        val alreadyActive = record.status == ProviderConnectionStatus.ACTIVE
        val result = try {
            module.completeAuthorization(
                OAuthCallbackRequest(
                    connectionId = stateRecord.connectionId,
                    userId = stateRecord.userId,
                    config = record.config,
                    currentCredentials = currentSecrets,
                    redirectUri = redirectUri,
                    code = queryParams["code"],
                    queryParams = queryParams,
                )
            )
        } catch (e: Exception) {
            if (alreadyActive) {
                log.info(
                    "OAuth callback ignored for already-ACTIVE connection {} (provider={}): {}",
                    stateRecord.connectionId, stateRecord.providerKey, e.javaClass.simpleName,
                )
                return OAuthCompletionResult.Success(stateRecord.connectionId)
            }
            log.warn("OAuth completion failed for connection {}", stateRecord.connectionId, e)
            repository.markSyncError(
                stateRecord.connectionId,
                "OAuth completion failed: ${e.message ?: e.javaClass.simpleName}",
            )
            return OAuthCompletionResult.Failure(stateRecord.connectionId, ERROR_PROVIDER)
        }

        val mergedSecrets = currentSecrets + result.envelope.toMap() + result.extraCredentials
        repository.update(
            authenticatedUser = callbackUser,
            id = stateRecord.connectionId,
            displayName = record.displayName,
            status = ProviderConnectionStatus.ACTIVE,
            config = record.config + result.configPatch,
            credentials = cipher.encrypt(mergedSecrets),
        ) ?: return OAuthCompletionResult.Failure(stateRecord.connectionId, ERROR_PERSIST)

        log.info("OAuth completion success: connection={} provider={}",
            stateRecord.connectionId, stateRecord.providerKey)
        return OAuthCompletionResult.Success(stateRecord.connectionId)
    }

    private fun resolveBaseUrl(): String {
        val raw = baseUrl.trim().removeSuffix("/")
        require(raw.isNotEmpty()) {
            "integrations.base-url (env INTEGRATIONS_BASE_URL) must be set for OAuth flows"
        }
        val uri = try { URI(raw) } catch (_: Exception) {
            throw IllegalStateException("integrations.base-url is not a valid URI: $raw")
        }
        val isLocalhost = uri.host?.equals("localhost", ignoreCase = true) == true ||
            uri.host == "127.0.0.1"
        require(uri.scheme == "https" || isLocalhost) {
            "integrations.base-url must use HTTPS (got $raw) — banks refuse insecure redirect URIs"
        }
        return raw
    }

    companion object {
        const val ERROR_STATE_INVALID = "state_invalid"
        const val ERROR_PROVIDER = "provider_error"
        const val ERROR_PROVIDER_GONE = "provider_gone"
        const val ERROR_PROVIDER_MISMATCH = "provider_mismatch"
        const val ERROR_CONNECTION_GONE = "connection_gone"
        const val ERROR_PERSIST = "persist_failed"
    }
}
