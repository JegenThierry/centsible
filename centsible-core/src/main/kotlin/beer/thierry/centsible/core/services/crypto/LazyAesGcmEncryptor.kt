package beer.thierry.centsible.core.services.crypto

import org.springframework.security.crypto.encrypt.BytesEncryptor
import org.springframework.security.crypto.encrypt.Encryptors

/**
 * Shared 256-bit AES-GCM encryptor (Spring Security Crypto's [Encryptors.stronger]) built lazily
 * from an env-supplied key/salt pair. Lazy construction lets a dev checkout with no configured
 * secrets boot; the first encrypt/decrypt fails loudly with the caller-supplied messages.
 *
 * Each at-rest cipher (TOTP secrets, provider credentials, …) wraps one of these with its own
 * config keys and domain-specific (de)serialization, so the key/salt validation and lazy-build
 * convention live in exactly one place.
 */
class LazyAesGcmEncryptor(
    private val key: String,
    private val salt: String,
    private val keyErrorMessage: String,
    private val saltErrorMessage: String,
) {
    private val delegate: BytesEncryptor by lazy {
        require(key.isNotBlank()) { keyErrorMessage }
        require(salt.matches(HEX_SALT_REGEX)) { saltErrorMessage }
        Encryptors.stronger(key, salt)
    }

    /** True when a key is present; does not validate the salt or build the encryptor. */
    val isConfigured: Boolean get() = key.isNotBlank()

    /** Forces lazy construction so misconfiguration surfaces now (e.g. a boot-time check). */
    fun ensureReady() {
        delegate
    }

    fun encrypt(data: ByteArray): ByteArray = delegate.encrypt(data)

    fun decrypt(data: ByteArray): ByteArray = delegate.decrypt(data)

    private companion object {
        val HEX_SALT_REGEX = Regex("^[0-9a-fA-F]{16,}$")
    }
}
