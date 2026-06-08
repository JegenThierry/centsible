package beer.thierry.centsible.core.services.authentication

import beer.thierry.centsible.core.services.crypto.LazyAesGcmEncryptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Encrypts/decrypts the TOTP shared secret at rest with 256-bit AES-GCM (Spring Security Crypto's
 * Encryptors.stronger). The secret must be reversible — the server recomputes codes on every
 * challenge — so it is encrypted, never hashed. The key/salt come from env (MFA_ENCRYPTION_KEY /
 * MFA_ENCRYPTION_SALT) and must live outside the database; ProductionGuard fails the prod boot if
 * they are missing or left at their .env.example placeholders.
 *
 * Built lazily so a dev checkout with no 2FA users still boots; the first enrollment fails loudly
 * if the key is unset.
 */
@Component
class TotpSecretCipher(
    @Value("\${mfa.encryption-key:}") encryptionKey: String,
    @Value("\${mfa.encryption-salt:}") encryptionSalt: String,
) {
    private val log = LoggerFactory.getLogger(TotpSecretCipher::class.java)

    private val encryptor = LazyAesGcmEncryptor(
        key = encryptionKey,
        salt = encryptionSalt,
        keyErrorMessage = "mfa.encryption-key (env MFA_ENCRYPTION_KEY) is not set — refusing to handle TOTP secrets",
        saltErrorMessage = "mfa.encryption-salt (env MFA_ENCRYPTION_SALT) must be a hex string of at least 16 chars",
    )

    init {
        if (!encryptor.isConfigured) {
            log.warn("mfa.encryption-key is not configured; two-factor enrollment will fail until it is set")
        }
    }

    fun encrypt(secret: String): ByteArray = encryptor.encrypt(secret.toByteArray(Charsets.UTF_8))

    fun decrypt(ciphertext: ByteArray): String = String(encryptor.decrypt(ciphertext), Charsets.UTF_8)
}
