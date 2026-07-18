package beer.thierry.centsiblerest.security

import tools.jackson.databind.json.JsonMapper
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AuthRateLimitFilterTest {

    private val filter = AuthRateLimitFilter(JsonMapper.builder().build())

    private fun loginRequest(ip: String, username: String): MockHttpServletRequest =
        MockHttpServletRequest("POST", "/api/auth/login").apply {
            remoteAddr = ip
            contentType = MediaType.APPLICATION_JSON_VALUE
            setContent("""{"username":"$username","password":"secret"}""".toByteArray())
        }

    /** Runs the filter and returns the body the downstream chain actually received, or null if blocked. */
    private fun passThroughBody(request: MockHttpServletRequest): String? {
        val chain = MockFilterChain()
        filter.doFilter(request, MockHttpServletResponse(), chain)
        val forwarded = chain.request ?: return null
        return forwarded.inputStream.readAllBytes().decodeToString()
    }

    @Test
    fun `replays the consumed body so the controller still binds it`() {
        val body = passThroughBody(loginRequest("10.0.0.1", "ada"))

        assertEquals("""{"username":"ada","password":"secret"}""", body)
    }

    @Test
    fun `reader replays the consumed body too`() {
        val chain = MockFilterChain()
        filter.doFilter(loginRequest("10.0.0.2", "ada"), MockHttpServletResponse(), chain)

        assertEquals("""{"username":"ada","password":"secret"}""", chain.request!!.reader.readText())
    }

    @Test
    fun `throttles one account across many source addresses`() {
        repeat(10) { i ->
            val response = MockHttpServletResponse()
            filter.doFilter(loginRequest("10.1.0.$i", "grace"), response, MockFilterChain())
            assertEquals(HttpStatus.OK.value(), response.status, "attempt $i should pass")
        }

        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(loginRequest("10.1.0.99", "grace"), response, chain)

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.status)
        assertNull(chain.request, "the 11th attempt must not reach the controller")
    }

    @Test
    fun `throttles one address walking many accounts`() {
        repeat(10) { i ->
            val response = MockHttpServletResponse()
            filter.doFilter(loginRequest("10.2.0.1", "user$i"), response, MockFilterChain())
            assertEquals(HttpStatus.OK.value(), response.status, "attempt $i should pass")
        }

        val response = MockHttpServletResponse()
        filter.doFilter(loginRequest("10.2.0.1", "user99"), response, MockFilterChain())

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.status)
    }

    @Test
    fun `a different account from a fresh address is unaffected by another account's limit`() {
        repeat(10) { i -> filter.doFilter(loginRequest("10.3.0.$i", "victim"), MockHttpServletResponse(), MockFilterChain()) }

        val response = MockHttpServletResponse()
        filter.doFilter(loginRequest("10.3.1.1", "bystander"), response, MockFilterChain())

        assertEquals(HttpStatus.OK.value(), response.status)
    }

    @Test
    fun `an unparseable body falls back to the address bucket instead of failing the request`() {
        val request = MockHttpServletRequest("POST", "/api/auth/login").apply {
            remoteAddr = "10.4.0.1"
            contentType = MediaType.APPLICATION_JSON_VALUE
            setContent("not json at all".toByteArray())
        }

        val chain = MockFilterChain()
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, chain)

        assertEquals(HttpStatus.OK.value(), response.status)
        assertEquals("not json at all", chain.request!!.inputStream.readAllBytes().decodeToString())
    }

    @Test
    fun `the totp challenge is keyed on its pre-auth cookie`() {
        fun challenge(ip: String, token: String) = MockHttpServletRequest("POST", "/api/auth/2fa/challenge").apply {
            remoteAddr = ip
            setCookies(Cookie(PRE_AUTH_COOKIE_NAME, token))
        }

        repeat(10) { i ->
            val response = MockHttpServletResponse()
            filter.doFilter(challenge("10.5.0.$i", "pending-token"), response, MockFilterChain())
            assertEquals(HttpStatus.OK.value(), response.status, "attempt $i should pass")
        }

        val response = MockHttpServletResponse()
        filter.doFilter(challenge("10.5.0.99", "pending-token"), response, MockFilterChain())

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.status)
    }

    @Test
    fun `a non json body is left unread and throttled on address alone`() {
        val request = MockHttpServletRequest("POST", "/api/auth/login").apply {
            remoteAddr = "10.6.0.1"
            contentType = MediaType.TEXT_PLAIN_VALUE
            setContent("username=ada".toByteArray())
        }

        val chain = MockFilterChain()
        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertEquals("username=ada", chain.request!!.inputStream.readAllBytes().decodeToString())
    }

    @Test
    fun `paths outside the auth surface are not filtered`() {
        val request = MockHttpServletRequest("GET", "/api/transactions").apply { remoteAddr = "10.7.0.1" }

        repeat(20) {
            val response = MockHttpServletResponse()
            filter.doFilter(request, response, MockFilterChain())
            assertEquals(HttpStatus.OK.value(), response.status)
        }
    }
}
