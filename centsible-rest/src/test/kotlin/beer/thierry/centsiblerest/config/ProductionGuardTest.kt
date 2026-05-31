package beer.thierry.centsiblerest.config

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.core.env.Environment

class ProductionGuardTest {

    private fun env(profiles: Array<String>): Environment {
        val env = mock(Environment::class.java)
        `when`(env.activeProfiles).thenReturn(profiles)
        return env
    }

    @Test
    fun `non-prod profile tolerates skip-email-verification and insecure cookies`() {
        // No exception expected — these flags are intentionally allowed outside prod for local dev.
        ProductionGuard(
            environment = env(arrayOf("dev")),
            skipEmailVerification = true,
            cookieSecure = false,
        ).assertSafeProduction()
    }

    @Test
    fun `prod profile rejects skip-email-verification`() {
        val guard = ProductionGuard(
            environment = env(arrayOf("prod")),
            skipEmailVerification = true,
            cookieSecure = true,
        )
        val ex = assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
        assert(ex.message!!.contains("skip.email.verification"))
    }

    @Test
    fun `prod profile rejects insecure auth cookie`() {
        val guard = ProductionGuard(
            environment = env(arrayOf("prod")),
            skipEmailVerification = false,
            cookieSecure = false,
        )
        val ex = assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
        assert(ex.message!!.contains("auth.cookie.secure"))
    }

    @Test
    fun `production profile alias is also caught (case-insensitive)`() {
        val guard = ProductionGuard(
            environment = env(arrayOf("Production")),
            skipEmailVerification = true,
            cookieSecure = true,
        )
        assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
    }

    @Test
    fun `prod profile with safe settings passes`() {
        ProductionGuard(
            environment = env(arrayOf("prod")),
            skipEmailVerification = false,
            cookieSecure = true,
        ).assertSafeProduction()
    }

    @Test
    fun `prod profile rejects the committed env-example encryption key`() {
        val guard = ProductionGuard(
            environment = env(arrayOf("prod")),
            skipEmailVerification = false,
            cookieSecure = true,
            encryptionKey = "change_me_long_random_passphrase",
        )
        val ex = assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
        assert(ex.message!!.contains("INTEGRATIONS_ENCRYPTION_KEY"))
    }

    @Test
    fun `prod profile rejects the committed env-example encryption salt`() {
        val guard = ProductionGuard(
            environment = env(arrayOf("prod")),
            skipEmailVerification = false,
            cookieSecure = true,
            encryptionSalt = "deadbeefcafebabe1234567890abcdef",
        )
        val ex = assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
        assert(ex.message!!.contains("INTEGRATIONS_ENCRYPTION_SALT"))
    }

    @Test
    fun `prod profile rejects the committed env-example jwt secret`() {
        val guard = ProductionGuard(
            environment = env(arrayOf("prod")),
            skipEmailVerification = false,
            cookieSecure = true,
            jwtSecret = "your-secret-here",
        )
        val ex = assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
        assert(ex.message!!.contains("JWT_SECRET"))
    }

    @Test
    fun `prod profile rejects the committed env-example database password`() {
        val guard = ProductionGuard(
            environment = env(arrayOf("prod")),
            skipEmailVerification = false,
            cookieSecure = true,
            datasourcePassword = "change_me_super_strong_password",
        )
        val ex = assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
        assert(ex.message!!.contains("POSTGRES_PASSWORD"))
    }

    @Test
    fun `non-prod profile tolerates the committed env-example placeholders`() {
        // Outside prod the placeholders are fine for local dev — the guard must not fire.
        ProductionGuard(
            environment = env(arrayOf("dev")),
            skipEmailVerification = false,
            cookieSecure = false,
            jwtSecret = "your-secret-here",
            encryptionKey = "change_me_long_random_passphrase",
            encryptionSalt = "deadbeefcafebabe1234567890abcdef",
            datasourcePassword = "change_me_super_strong_password",
        ).assertSafeProduction()
    }

    @Test
    fun `prod profile passes when real secrets replace the placeholders`() {
        ProductionGuard(
            environment = env(arrayOf("prod")),
            skipEmailVerification = false,
            cookieSecure = true,
            jwtSecret = "a-freshly-generated-long-base64-secret",
            encryptionKey = "a-real-strong-passphrase",
            encryptionSalt = "00112233445566778899aabbccddeeff",
            datasourcePassword = "an-actual-strong-db-password",
        ).assertSafeProduction()
    }
}
