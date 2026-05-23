package beer.thierry.centsiblerest.security

import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class AuthCookieIssuerTest {

    private fun headers(response: HttpServletResponse): String {
        val captor = ArgumentCaptor.forClass(String::class.java)
        verify(response).addHeader(org.mockito.ArgumentMatchers.eq("Set-Cookie"), captor.capture())
        return captor.value
    }

    @Test
    fun `issue sets HttpOnly Strict cookie with the requested max-age`() {
        val response = mock(HttpServletResponse::class.java)
        val issuer = AuthCookieIssuer(
            jwtExpirationMs = 3_600_000,
            cookieSecure = false,
            cookieDomain = "",
        )

        issuer.issue(response, "token-value")

        val cookie = headers(response)
        assertTrue(cookie.startsWith("$AUTH_COOKIE_NAME=token-value"))
        assertTrue(cookie.contains("HttpOnly"))
        assertTrue(cookie.contains("SameSite=Strict"))
        assertTrue(cookie.contains("Path=/"))
        assertTrue(cookie.contains("Max-Age=3600"))
    }

    @Test
    fun `issue marks the cookie Secure under prod-style configuration`() {
        val response = mock(HttpServletResponse::class.java)
        val issuer = AuthCookieIssuer(
            jwtExpirationMs = 1000,
            cookieSecure = true,
            cookieDomain = "",
        )

        issuer.issue(response, "tok")

        val cookie = headers(response)
        assertTrue(cookie.contains("Secure"))
    }

    @Test
    fun `issue applies a custom domain when configured`() {
        val response = mock(HttpServletResponse::class.java)
        val issuer = AuthCookieIssuer(
            jwtExpirationMs = 1000,
            cookieSecure = false,
            cookieDomain = "centsible.example",
        )

        issuer.issue(response, "tok")

        val cookie = headers(response)
        assertTrue(cookie.contains("Domain=centsible.example"))
    }

    @Test
    fun `clear writes a zero-length cookie with Max-Age 0`() {
        val response = mock(HttpServletResponse::class.java)
        val issuer = AuthCookieIssuer(
            jwtExpirationMs = 1000,
            cookieSecure = false,
            cookieDomain = "",
        )

        issuer.clear(response)

        val cookie = headers(response)
        assertNotNull(cookie)
        assertTrue(cookie.startsWith("$AUTH_COOKIE_NAME="))
        // Value cleared and Max-Age=0 — the browser must drop it.
        assertEquals(true, cookie.contains("Max-Age=0"))
    }
}
