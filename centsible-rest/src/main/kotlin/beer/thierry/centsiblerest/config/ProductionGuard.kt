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
        }
        if (skipEmailVerification) {
            log.warn("skip.email.verification=true — accounts will be auto-confirmed without email verification. Never enable this in production.")
        }
    }
}
