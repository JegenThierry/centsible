package beer.thierry.centsible.integrations.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class IntegrationApiExceptionTest {

    @Test
    fun `message includes provider status body and context`() {
        val ex = IntegrationApiException(
            provider = "PayPal",
            status = HttpStatus.BAD_GATEWAY,
            body = "upstream gateway timeout",
            context = "oauth2/token",
        )

        val msg = ex.message!!
        assertTrue(msg.contains("PayPal"))
        assertTrue(msg.contains("502"))
        assertTrue(msg.contains("oauth2/token"))
        assertTrue(msg.contains("upstream gateway timeout"))
    }

    @Test
    fun `message omits context segment when no context provided`() {
        val ex = IntegrationApiException(
            provider = "GoCardless",
            status = HttpStatus.NOT_FOUND,
            body = "not found",
        )

        assertTrue(ex.message!!.contains("GoCardless"))
        assertTrue(ex.message!!.contains("404"))
        assertTrue(!ex.message!!.contains("(") && !ex.message!!.contains(")"))
    }

    @Test
    fun `bodySnippet is truncated to MAX_BODY_SNIPPET characters`() {
        val longBody = "x".repeat(IntegrationApiException.MAX_BODY_SNIPPET + 200)

        val ex = IntegrationApiException(
            provider = "X",
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            body = longBody,
        )

        assertEquals(IntegrationApiException.MAX_BODY_SNIPPET, ex.bodySnippet.length)
    }

    @Test
    fun `bodySnippet is empty when body is null`() {
        val ex = IntegrationApiException(
            provider = "X",
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            body = null,
        )

        assertEquals("", ex.bodySnippet)
        assertNotNull(ex.message)
    }
}
