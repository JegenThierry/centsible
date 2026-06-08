package beer.thierry.centsible.api.model.integrations

import java.time.Instant

/**
 * Stable convention for OAuth2 token storage inside the encrypted credentials Map<String,String>.
 *
 * Provider modules never read or write these raw keys directly — they go through the helpers below
 * and through TokenRefreshGuard which mutates the live envelope before each provider call.
 *
 * We pack everything into the existing credentials map (rather than adding new DB columns) so the
 * schema does not need to change every time a provider has a new token field.
 */
object OAuthCredentialKeys {
    const val ACCESS_TOKEN = "oauth.access_token"
    const val REFRESH_TOKEN = "oauth.refresh_token"
    const val TOKEN_TYPE = "oauth.token_type"
    const val EXPIRES_AT_EPOCH = "oauth.expires_at"
    const val SCOPE = "oauth.scope"
    const val OBTAINED_AT_EPOCH = "oauth.obtained_at"
}

data class OAuthCredentialEnvelope(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresAt: Instant? = null,
    val scope: String? = null,
    val obtainedAt: Instant = Instant.now(),
) {
    /** True if the token expires within [safetyWindowSeconds]; always false when no expiry is known. */
    fun isExpired(safetyWindowSeconds: Long = 60): Boolean {
        val expiry = expiresAt ?: return false
        return Instant.now().plusSeconds(safetyWindowSeconds).isAfter(expiry)
    }

    fun toMap(): Map<String, String> = buildMap {
        put(OAuthCredentialKeys.ACCESS_TOKEN, accessToken)
        put(OAuthCredentialKeys.TOKEN_TYPE, tokenType)
        put(OAuthCredentialKeys.OBTAINED_AT_EPOCH, obtainedAt.epochSecond.toString())
        refreshToken?.let { put(OAuthCredentialKeys.REFRESH_TOKEN, it) }
        expiresAt?.let { put(OAuthCredentialKeys.EXPIRES_AT_EPOCH, it.epochSecond.toString()) }
        scope?.let { put(OAuthCredentialKeys.SCOPE, it) }
    }

    companion object {
        /** Parses an envelope out of a raw credentials map; null when no access token is present. */
        fun from(credentials: Map<String, String>): OAuthCredentialEnvelope? {
            val accessToken = credentials[OAuthCredentialKeys.ACCESS_TOKEN] ?: return null
            return OAuthCredentialEnvelope(
                accessToken = accessToken,
                refreshToken = credentials[OAuthCredentialKeys.REFRESH_TOKEN],
                tokenType = credentials[OAuthCredentialKeys.TOKEN_TYPE] ?: "Bearer",
                expiresAt = credentials[OAuthCredentialKeys.EXPIRES_AT_EPOCH]?.toLongOrNull()
                    ?.let(Instant::ofEpochSecond),
                scope = credentials[OAuthCredentialKeys.SCOPE],
                obtainedAt = credentials[OAuthCredentialKeys.OBTAINED_AT_EPOCH]?.toLongOrNull()
                    ?.let(Instant::ofEpochSecond) ?: Instant.now(),
            )
        }
    }
}
