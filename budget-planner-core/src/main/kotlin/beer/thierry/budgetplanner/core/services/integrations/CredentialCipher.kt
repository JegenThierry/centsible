package beer.thierry.budgetplanner.core.services.integrations

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.encrypt.BytesEncryptor
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.stereotype.Component

/**
 * Encrypts and decrypts the per-connection credentials blob (a Map<String, String> of secret
 * fields) at rest. Uses Spring Security Crypto's 256-bit AES-GCM via Encryptors.stronger.
 *
 * Both the password and the salt come from env (INTEGRATIONS_ENCRYPTION_KEY /
 * INTEGRATIONS_ENCRYPTION_SALT). The encryptor is built lazily so a fresh dev checkout with
 * no integrations configured does not break startup — but the first encrypt/decrypt fails fast
 * with a clear error.
 */
@Component
class CredentialCipher(
    @Value("\${integrations.encryption-key:}") private val encryptionKey: String,
    @Value("\${integrations.encryption-salt:}") private val encryptionSalt: String,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(CredentialCipher::class.java)

    private val encryptor: BytesEncryptor by lazy {
        require(encryptionKey.isNotBlank()) {
            "integrations.encryption-key (env INTEGRATIONS_ENCRYPTION_KEY) is not set — refusing to handle credentials"
        }
        require(encryptionSalt.matches(HEX_SALT_REGEX)) {
            "integrations.encryption-salt (env INTEGRATIONS_ENCRYPTION_SALT) must be a hex string of at least 16 chars"
        }
        Encryptors.stronger(encryptionKey, encryptionSalt)
    }

    private val mapType = object : TypeReference<Map<String, String>>() {}

    init {
        if (encryptionKey.isBlank()) {
            log.warn("integrations.encryption-key is not configured; provider connections that store secrets will fail until it is set")
        }
    }

    fun encrypt(credentials: Map<String, String>): ByteArray? {
        if (credentials.isEmpty()) return null
        return encryptor.encrypt(objectMapper.writeValueAsBytes(credentials))
    }

    fun decrypt(ciphertext: ByteArray?): Map<String, String> {
        if (ciphertext == null || ciphertext.isEmpty()) return emptyMap()
        return objectMapper.readValue(encryptor.decrypt(ciphertext), mapType)
    }

    companion object {
        private val HEX_SALT_REGEX = Regex("^[0-9a-fA-F]{16,}$")
    }
}
