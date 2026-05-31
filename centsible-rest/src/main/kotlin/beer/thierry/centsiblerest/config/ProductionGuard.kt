package beer.thierry.centsiblerest.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class ProductionGuard(
    private val environment: Environment,
    @Value("\${skip.email.verification:false}") private val skipEmailVerification: Boolean,
    @Value("\${auth.cookie.secure:false}") private val cookieSecure: Boolean,
    @Value("\${jwt.secret:}") private val jwtSecret: String = "",
    @Value("\${integrations.encryption-key:}") private val encryptionKey: String = "",
    @Value("\${integrations.encryption-salt:}") private val encryptionSalt: String = "",
    @Value("\${spring.datasource.password:}") private val datasourcePassword: String = "",
) {
    private val log = LoggerFactory.getLogger(ProductionGuard::class.java)

    @PostConstruct
    fun assertSafeProduction() {
        val isProd = environment.activeProfiles.any {
            it.equals("prod", ignoreCase = true) || it.equals("production", ignoreCase = true)
        }
        if (isProd) {
            require(!skipEmailVerification) {
                "skip.email.verification=true is not allowed under the prod profile. " +
                    "Unset SKIP_EMAIL_VERIFICATION before starting."
            }
            require(cookieSecure) {
                "auth.cookie.secure must be true under the prod profile. " +
                    "Set AUTH_COOKIE_SECURE=true and front the stack with HTTPS."
            }
            // Reject the publicly-known placeholders shipped in .env.example. Because .env.example sets
            // SPRING_PROFILES_ACTIVE=prod, an operator who copies it and replaces only the obvious DB
            // password would otherwise boot with a credential-encryption key, KDF salt and JWT secret
            // that are committed to a public repo (and therefore not secret).
            rejectPlaceholder("JWT_SECRET", "jwt.secret", jwtSecret, PLACEHOLDER_JWT_SECRET)
            rejectPlaceholder("INTEGRATIONS_ENCRYPTION_KEY", "integrations.encryption-key", encryptionKey, PLACEHOLDER_ENCRYPTION_KEY)
            rejectPlaceholder("INTEGRATIONS_ENCRYPTION_SALT", "integrations.encryption-salt", encryptionSalt, PLACEHOLDER_ENCRYPTION_SALT)
            rejectPlaceholder("POSTGRES_PASSWORD", "spring.datasource.password", datasourcePassword, PLACEHOLDER_DB_PASSWORD)
        }
        if (skipEmailVerification) {
            log.warn("skip.email.verification=true — accounts will be auto-confirmed without email verification. Never enable this in production.")
        }
    }

    private fun rejectPlaceholder(envName: String, property: String, actual: String, placeholder: String) {
        require(actual != placeholder) {
            "$envName ($property) is still set to the placeholder value shipped in .env.example. " +
                "That value is committed to a public repository and is NOT secret — generate a fresh " +
                "value before starting under the prod profile."
        }
    }

    private companion object {
        const val PLACEHOLDER_JWT_SECRET = "your-secret-here"
        const val PLACEHOLDER_ENCRYPTION_KEY = "change_me_long_random_passphrase"
        const val PLACEHOLDER_ENCRYPTION_SALT = "deadbeefcafebabe1234567890abcdef"
        const val PLACEHOLDER_DB_PASSWORD = "change_me_super_strong_password"
    }
}
