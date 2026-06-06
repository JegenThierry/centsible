package beer.thierry.centsible.api.services.integrations

import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

/**
 * Server-side coordinator for the OAuth2 authorization-code flow used by providers like
 * GoCardless Bank Account Data. State is HMAC-signed (no DB row needed) and tied to the
 * connection + user. The OAuth callback endpoint is public-by-necessity — security comes
 * from state verification, not from authentication on the callback URL.
 */
interface IOAuthFlowService {
    /**
     * Begins the flow for an existing connection. Validates the user owns the connection,
     * delegates URL construction to the provider, returns the URL the browser should follow.
     */
    fun startAuthorization(authenticatedUser: UserDTO, connectionId: UUID): OAuthStartResult

    /**
     * Handles the redirect back from the provider. Validates state (signature + expiry +
     * provider key match), exchanges the code via the provider, persists the resulting
     * envelope (encrypted) + config patch, and flips the connection to ACTIVE.
     *
     * Returns the connectionId so the caller can build a redirect-back URL for the browser.
     */
    fun completeAuthorization(
        providerKey: String,
        state: String,
        queryParams: Map<String, String>,
    ): OAuthCompletionResult
}

data class OAuthStartResult(
    val authorizationUrl: String,
    val connectionId: UUID,
)

sealed class OAuthCompletionResult {
    data class Success(val connectionId: UUID) : OAuthCompletionResult()
    data class Failure(val connectionId: UUID?, val reason: String) : OAuthCompletionResult()
}
