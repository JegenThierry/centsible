package beer.thierry.centsible.api.services.integrations

import beer.thierry.centsible.api.model.integrations.ExternalAccountDTO
import beer.thierry.centsible.api.model.integrations.OAuthCallbackRequest
import beer.thierry.centsible.api.model.integrations.OAuthCredentialEnvelope
import beer.thierry.centsible.api.model.integrations.OAuthStartRequest
import beer.thierry.centsible.api.model.integrations.ProviderContext
import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.model.integrations.QuoteDTO
import beer.thierry.centsible.api.model.integrations.RemoteOptionsRequest
import beer.thierry.centsible.api.model.integrations.SelectOption
import beer.thierry.centsible.api.model.integrations.TransactionImportPage

/**
 * Base contract every integration provider implements. Capability interfaces below are mixins —
 * a provider only implements those that apply. Provider modules depend only on
 * centsible-api and are auto-discovered via Spring component scanning.
 */
interface ProviderModule {
    val descriptor: ProviderDescriptor

    val minSyncIntervalSeconds: Long get() = 0L

    /**
     * Lightweight reachability check executed when a user creates or edits a connection.
     * Should not perform a full sync. Default: no-op (provider accepts any config).
     */
    fun testConnection(ctx: ProviderContext) {}
}

interface IAccountProvider {
    fun listExternalAccounts(ctx: ProviderContext): List<ExternalAccountDTO>
}

interface ITransactionImporter {
    fun importSince(ctx: ProviderContext, cursor: String?): TransactionImportPage
}

interface IQuoteProvider {
    fun fetchQuotes(ctx: ProviderContext, symbols: List<String>): List<QuoteDTO>
}

/**
 * Implemented by providers that use the OAuth2 authorization-code flow (e.g. GoCardless's
 * end-user agreement / requisition consent). The orchestrator handles state signing and
 * redirect-URI assembly; provider modules only build the consent URL and finish the exchange.
 */
interface IOAuthFlowProvider {
    /**
     * Build the consent URL to redirect the user to. Returned `configPatch` is persisted into
     * the connection's config BEFORE the browser is redirected — useful for stashing IDs (like
     * a GoCardless requisitionId) that the callback needs to know about. The patch is merged,
     * not replaced.
     */
    fun buildAuthorizationUrl(request: OAuthStartRequest): OAuthAuthorizationStart

    /**
     * Completes the OAuth handshake when the user is redirected back. Returns the token
     * envelope (and any additional credentials/config the provider needs to persist).
     */
    fun completeAuthorization(request: OAuthCallbackRequest): OAuthCallbackResult

    /**
     * Called by TokenRefreshGuard when the stored envelope is within the safety window. Must
     * return a fresh envelope or throw. Default: no refresh (treat tokens as long-lived).
     */
    fun refreshAccessToken(ctx: ProviderContext): OAuthCredentialEnvelope =
        OAuthCredentialEnvelope.from(ctx.credentials)
            ?: throw IllegalStateException("Cannot refresh: no OAuth envelope in credentials")
}

data class OAuthAuthorizationStart(
    val authorizationUrl: String,
    val configPatch: Map<String, Any?> = emptyMap(),
)

data class OAuthCallbackResult(
    val envelope: OAuthCredentialEnvelope,
    val configPatch: Map<String, Any?> = emptyMap(),
    val extraCredentials: Map<String, String> = emptyMap(),
)

/**
 * Implemented by providers with SELECT_REMOTE config fields. Allows the UI to ask for
 * institution lists, account pickers, etc. that depend on other field values.
 */
interface IRemoteOptionsProvider {
    fun fetchOptions(ctx: ProviderContext, request: RemoteOptionsRequest): List<SelectOption>
}
