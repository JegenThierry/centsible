package beer.thierry.centsible.core.services.integrations

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import java.util.UUID

@Component
class OAuthStateStore {

    private val random = SecureRandom()

    private val cache: Cache<String, StateRecord> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(TTL_MINUTES))
        .maximumSize(MAX_INFLIGHT_TOKENS)
        .build()

    fun mint(userId: UUID, connectionId: UUID, providerKey: String): String {
        val token = generateToken()
        cache.put(token, StateRecord(userId, connectionId, providerKey))
        return token
    }

    /**
     * Atomic consume-on-use: returns the record on the first call within the TTL and null on
     * any subsequent call (including a same-millisecond race). The expectedProviderKey check is
     * a defense-in-depth — a token minted for provider A cannot complete provider B's callback
     * even if the URL routing somehow misfires.
     */
    fun consume(token: String, expectedProviderKey: String): StateRecord? {
        if (token.isBlank()) return null
        val record = cache.asMap().remove(token) ?: return null
        if (record.providerKey != expectedProviderKey) return null
        return record
    }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    data class StateRecord(
        val userId: UUID,
        val connectionId: UUID,
        val providerKey: String,
    )

    private companion object {
        const val TTL_MINUTES = 10L
        const val MAX_INFLIGHT_TOKENS = 10_000L
        const val TOKEN_BYTES = 16
    }
}
