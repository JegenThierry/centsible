package beer.thierry.centsiblerest.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

const val AUTH_COOKIE_NAME = "auth_token"
const val PRE_AUTH_COOKIE_NAME = "pre_auth"

/**
 * Builds a hardened `Set-Cookie` value shared by both issuers below: always HttpOnly + SameSite=Strict,
 * with `secure`/`domain` applied in exactly one place. Only the name, path and max-age vary per issuer.
 */
private fun buildSetCookie(
    name: String,
    value: String,
    path: String,
    maxAge: Duration,
    secure: Boolean,
    domain: String,
): String = ResponseCookie.from(name, value)
    .httpOnly(true)
    .secure(secure)
    .sameSite("Strict")
    .path(path)
    .maxAge(maxAge)
    .also { if (domain.isNotBlank()) it.domain(domain) }
    .build()
    .toString()

@Component
class AuthCookieIssuer(
    @Value("\${jwt.expiration-ms}") private val jwtExpirationMs: Long,
    @Value("\${auth.cookie.secure:false}") private val cookieSecure: Boolean,
    @Value("\${auth.cookie.domain:}") private val cookieDomain: String,
) {
    fun issue(response: HttpServletResponse, token: String) {
        response.addHeader("Set-Cookie", cookie(token, Duration.ofMillis(jwtExpirationMs)))
    }

    fun clear(response: HttpServletResponse) {
        response.addHeader("Set-Cookie", cookie("", Duration.ZERO))
    }

    private fun cookie(value: String, maxAge: Duration) =
        buildSetCookie(AUTH_COOKIE_NAME, value, "/", maxAge, cookieSecure, cookieDomain)
}

@Component
class PreAuthCookieIssuer(
    @Value("\${auth.cookie.secure:false}") private val cookieSecure: Boolean,
    @Value("\${auth.cookie.domain:}") private val cookieDomain: String,
) {
    fun issue(response: HttpServletResponse, token: String) {
        response.addHeader("Set-Cookie", cookie(token, PENDING_TTL))
    }

    fun clear(response: HttpServletResponse) {
        response.addHeader("Set-Cookie", cookie("", Duration.ZERO))
    }

    private fun cookie(value: String, maxAge: Duration) =
        buildSetCookie(PRE_AUTH_COOKIE_NAME, value, "/api/auth", maxAge, cookieSecure, cookieDomain)

    private companion object {
        val PENDING_TTL: Duration = Duration.ofMinutes(5)
    }
}
