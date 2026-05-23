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
}
