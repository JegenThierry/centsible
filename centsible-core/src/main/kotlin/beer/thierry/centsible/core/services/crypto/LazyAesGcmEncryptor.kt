package beer.thierry.centsible.core.services.crypto

import org.springframework.security.crypto.encrypt.BytesEncryptor
import org.springframework.security.crypto.encrypt.Encryptors

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
