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

    /** A guard with every secret set to a realistic non-placeholder value; override one to test it. */
    private fun prodGuard(
        jwtSecret: String = "a-freshly-generated-long-base64-secret",
        mfaEncryptionKey: String = "another-real-strong-passphrase",
        mfaEncryptionSalt: String = "ffeeddccbbaa99887766554433221100",
        datasourcePassword: String = "an-actual-strong-db-password",
        adminEnabled: Boolean = false,
        adminUsername: String = "",
    ) = ProductionGuard(
        environment = env(arrayOf("prod")),
        skipEmailVerification = false,
        cookieSecure = true,
        jwtSecret = jwtSecret,
        mfaEncryptionKey = mfaEncryptionKey,
        mfaEncryptionSalt = mfaEncryptionSalt,
        datasourcePassword = datasourcePassword,
        adminEnabled = adminEnabled,
        adminUsername = adminUsername,
    )

    @Test
    fun `non-prod profile tolerates skip-email-verification and insecure cookies`() {
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
    fun `prod profile rejects empty secrets`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            ProductionGuard(
                environment = env(arrayOf("prod")),
                skipEmailVerification = false,
                cookieSecure = true,
            ).assertSafeProduction()
        }
        assert(ex.message!!.contains("is empty"))
    }

    @Test
    fun `prod profile rejects a single blank secret among otherwise real ones`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            prodGuard(mfaEncryptionKey = "").assertSafeProduction()
        }
        assert(ex.message!!.contains("MFA_ENCRYPTION_KEY"))
    }

    @Test
    fun `prod profile rejects the committed env-example jwt secret`() {
        val guard = prodGuard(jwtSecret = "your-secret-here")
        val ex = assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
        assert(ex.message!!.contains("JWT_SECRET"))
    }

    @Test
    fun `prod profile rejects the committed env-example database password`() {
        val guard = prodGuard(datasourcePassword = "change_me_super_strong_password")
        val ex = assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
        assert(ex.message!!.contains("POSTGRES_PASSWORD"))
    }

    @Test
    fun `non-prod profile tolerates the committed env-example placeholders`() {
        ProductionGuard(
            environment = env(arrayOf("dev")),
            skipEmailVerification = false,
            cookieSecure = false,
            jwtSecret = "your-secret-here",
            datasourcePassword = "change_me_super_strong_password",
        ).assertSafeProduction()
    }

    @Test
    fun `prod profile passes when real secrets replace the placeholders`() {
        prodGuard().assertSafeProduction()
    }

    @Test
    fun `prod profile rejects an enabled admin area without a username`() {
        val guard = prodGuard(adminEnabled = true, adminUsername = "")
        val ex = assertThrows(IllegalArgumentException::class.java) { guard.assertSafeProduction() }
        assert(ex.message!!.contains("ADMIN_USERNAME"))
    }

    @Test
    fun `prod profile passes with an enabled admin area and a username`() {
        prodGuard(adminEnabled = true, adminUsername = "admin").assertSafeProduction()
    }

    @Test
    fun `prod profile ignores the admin username while the feature is disabled`() {
        prodGuard(adminEnabled = false, adminUsername = "").assertSafeProduction()
    }
}
