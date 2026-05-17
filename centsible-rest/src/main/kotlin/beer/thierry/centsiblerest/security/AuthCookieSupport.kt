package beer.thierry.centsiblerest.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

const val AUTH_COOKIE_NAME = "auth_token"

// ProductionGuard enforces cookieSecure=true under the prod profile.
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
