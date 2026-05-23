package beer.thierry.centsible.api.model.integrations

import java.util.UUID

data class ProviderContext(
    val userId: UUID,
    val connectionId: UUID,
    val displayName: String,
    val config: Map<String, Any?>,
    /**
     * Encrypted-at-rest credential bag. For OAuth providers, the orchestrator populates the
     * `oauth.*` keys via [OAuthCredentialEnvelope.toMap] before invoking capability methods,
     * and refreshes them in place via TokenRefreshGuard. Provider modules should read tokens
     * with `OAuthCredentialEnvelope.from(credentials)` rather than reaching for raw keys.
     */
    val credentials: Map<String, String>,
    val lastCursor: String? = null,
)

/**
 * Passed to IRemoteOptionsProvider when the UI's SELECT_REMOTE dropdown asks for options.
 * `values` carries the in-progress form values so dependent dropdowns (e.g. institutionId
 * filtered by country) can narrow their result set.
 */
data class RemoteOptionsRequest(
    val fieldName: String,
    val query: String? = null,
    val values: Map<String, Any?> = emptyMap(),
)

/**
 * Inputs to the OAuth2 authorization-code dance. The orchestrator builds these from the
 * configured INTEGRATIONS_BASE_URL plus an HMAC-signed state token; provider modules only see
 * the finished URI + opaque state and use them to construct the bank's consent URL.
 */
data class OAuthStartRequest(
    val connectionId: UUID,
    val userId: UUID,
    val displayName: String,
    val config: Map<String, Any?>,
    val redirectUri: String,
    val state: String,
)

data class OAuthCallbackRequest(
    val connectionId: UUID,
    val userId: UUID,
    val config: Map<String, Any?>,
    val currentCredentials: Map<String, String>,
    val redirectUri: String,
    /** OAuth `code` query param from the callback (some providers don't use it; see GoCardless). */
    val code: String? = null,
    /** All query params from the callback, in case the provider needs more than `code`. */
    val queryParams: Map<String, String> = emptyMap(),
)
