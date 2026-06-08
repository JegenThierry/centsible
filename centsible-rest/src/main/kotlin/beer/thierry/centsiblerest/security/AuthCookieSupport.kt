package beer.thierry.centsiblerest.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

const val AUTH_COOKIE_NAME = "auth_token"
const val MFA_PENDING_COOKIE_NAME = "mfa_pending"

@Component
class AuthCookieIssuer(
    @Value("\${jwt.expiration-ms}") private val jwtExpirationMs: Long,
    @Value("\${auth.cookie.secure:false}") private val cookieSecure: Boolean,
    @Value("\${auth.cookie.domain:}") private val cookieDomain: String,
) {
    fun issue(response: HttpServletResponse, token: String) {
        val cookie = baseBuilder(token)
            .maxAge(Duration.ofMillis(jwtExpirationMs))
            .build()
        response.addHeader("Set-Cookie", cookie.toString())
    }

    fun clear(response: HttpServletResponse) {
        val cookie = baseBuilder("")
            .maxAge(Duration.ZERO)
            .build()
        response.addHeader("Set-Cookie", cookie.toString())
    }

    private fun baseBuilder(value: String) = ResponseCookie.from(AUTH_COOKIE_NAME, value)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Strict")
        .path("/")
        .also { if (cookieDomain.isNotBlank()) it.domain(cookieDomain) }
}

/**
 * Issues the short-lived pre-auth cookie that bridges password-verify and the TOTP challenge.
 * Distinct from the real session cookie: a different name, a path scoped to the auth endpoints,
 * and a 5-minute lifetime. It carries no authority of its own — the JwtAuthenticationFilter never
 * reads it — so it can never stand in for a real JWT.
 */
@Component
class MfaPendingCookieIssuer(
    @Value("\${auth.cookie.secure:false}") private val cookieSecure: Boolean,
    @Value("\${auth.cookie.domain:}") private val cookieDomain: String,
) {
    fun issue(response: HttpServletResponse, token: String) {
        response.addHeader("Set-Cookie", baseBuilder(token).maxAge(PENDING_TTL).build().toString())
    }

    fun clear(response: HttpServletResponse) {
        response.addHeader("Set-Cookie", baseBuilder("").maxAge(Duration.ZERO).build().toString())
    }

    private fun baseBuilder(value: String) = ResponseCookie.from(MFA_PENDING_COOKIE_NAME, value)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Strict")
        .path("/api/auth")
        .also { if (cookieDomain.isNotBlank()) it.domain(cookieDomain) }

    private companion object {
        val PENDING_TTL: Duration = Duration.ofMinutes(5)
    }
}
