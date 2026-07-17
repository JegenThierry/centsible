package beer.thierry.centsible.core.services.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LazyAesGcmEncryptorTest {

    private val key = "a-strong-passphrase"
    private val salt = "0123456789abcdef"

    private fun cipher(key: String = this.key, salt: String = this.salt) =
        LazyAesGcmEncryptor(key, salt, "key missing", "salt invalid")

    @Test
    fun `round-trips bytes through encrypt and decrypt`() {
        val cipher = cipher()
        val plaintext = "the quick brown fox".toByteArray()

        val decrypted = cipher.decrypt(cipher.encrypt(plaintext))

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `ciphertext is not the plaintext`() {
        val cipher = cipher()
        val plaintext = "secret".toByteArray()

        assertFalse(cipher.encrypt(plaintext).contentEquals(plaintext))
    }

    @Test
    fun `AES-GCM nonce makes repeated encryptions differ`() {
        val cipher = cipher()
        val plaintext = "secret".toByteArray()

        assertNotEquals(
            cipher.encrypt(plaintext).toList(),
            cipher.encrypt(plaintext).toList(),
        )
    }

    @Test
    fun `isConfigured reflects a present key without building the encryptor`() {
        assertTrue(cipher().isConfigured)
        assertFalse(cipher(key = "").isConfigured)
        assertFalse(cipher(key = " ").isConfigured)
    }

    @Test
    fun `blank key fails with the supplied message only on use`() {
        val cipher = cipher(key = "")
        val ex = assertThrows(IllegalArgumentException::class.java) { cipher.ensureReady() }
        assertEquals("key missing", ex.message)
    }

    @Test
    fun `non-hex or too-short salt fails with the supplied message`() {
        assertEquals(
            "salt invalid",
            assertThrows(IllegalArgumentException::class.java) { cipher(salt = "not-hex!!").ensureReady() }.message,
        )
        assertEquals(
            "salt invalid",
            assertThrows(IllegalArgumentException::class.java) { cipher(salt = "abc123").ensureReady() }.message,
        )
    }
}
