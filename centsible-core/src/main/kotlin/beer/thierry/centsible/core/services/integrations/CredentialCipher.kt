package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.services.integrations.IProviderRegistry
import beer.thierry.centsible.core.services.crypto.LazyAesGcmEncryptor
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class CredentialCipher(
    @Value("\${integrations.encryption-key:}") private val encryptionKey: String,
    @Value("\${integrations.encryption-salt:}") private val encryptionSalt: String,
    private val objectMapper: ObjectMapper,
    private val applicationContext: ApplicationContext,
) {
    private val log = LoggerFactory.getLogger(CredentialCipher::class.java)

    private val encryptor = LazyAesGcmEncryptor(
        key = encryptionKey,
        salt = encryptionSalt,
        keyErrorMessage = "integrations.encryption-key (env INTEGRATIONS_ENCRYPTION_KEY) is not set — refusing to handle credentials",
        saltErrorMessage = "integrations.encryption-salt (env INTEGRATIONS_ENCRYPTION_SALT) must be a hex string of at least 16 chars",
    )

    private val mapType = object : TypeReference<Map<String, String>>() {}

    init {
        if (!encryptor.isConfigured) {
            log.warn("integrations.encryption-key is not configured; provider connections that store secrets will fail until it is set")
        }
    }

    @PostConstruct
    fun validateAtBoot() {
        val registry = runCatching { applicationContext.getBean(IProviderRegistry::class.java) }
            .getOrNull() ?: return
        val needsCipher = registry.listDescriptors().any { it.authType != AuthType.NONE }
        if (!needsCipher) return
        try {
            encryptor.ensureReady()
            log.info("Credential cipher ready; {} credentialled provider(s) enabled.",
                registry.listDescriptors().count { it.authType != AuthType.NONE })
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(
                "At least one provider that stores credentials is enabled, but the credential " +
                    "cipher is not configured: ${e.message}",
                e,
            )
        }
    }

    fun encrypt(credentials: Map<String, String>): ByteArray? {
        if (credentials.isEmpty()) return null
        return try {
            encryptor.encrypt(objectMapper.writeValueAsBytes(credentials))
        } catch (e: Exception) {
            log.error("Credential encryption failed (fields={})", credentials.keys.sorted(), e)
            throw e
        }
    }

    fun decrypt(ciphertext: ByteArray?): Map<String, String> {
        if (ciphertext == null || ciphertext.isEmpty()) return emptyMap()
        return try {
            objectMapper.readValue(encryptor.decrypt(ciphertext), mapType)
        } catch (e: Exception) {
            log.error("Credential decryption failed (cipherBytes={})", ciphertext.size, e)
            throw e
        }
    }
}
