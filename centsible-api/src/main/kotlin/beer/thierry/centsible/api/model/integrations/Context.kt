package beer.thierry.centsible.api.model.integrations

import java.util.UUID

data class ProviderContext(
    val userId: UUID,
    val connectionId: UUID,
    val displayName: String,
    val config: Map<String, Any?>,
    val credentials: Map<String, String>,
    val lastCursor: String? = null,
)

data class RemoteOptionsRequest(
    val fieldName: String,
    val query: String? = null,
    val values: Map<String, Any?> = emptyMap(),
)

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
    val code: String? = null,
    val queryParams: Map<String, String> = emptyMap(),
)
