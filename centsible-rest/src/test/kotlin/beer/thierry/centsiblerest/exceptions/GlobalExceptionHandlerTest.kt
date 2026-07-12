package beer.thierry.centsiblerest.exceptions

import beer.thierry.centsiblerest.logging.MDC_REQUEST_ID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.slf4j.MDC
import org.springframework.context.support.StaticMessageSource
import org.springframework.web.context.request.WebRequest

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler(StaticMessageSource())
    private val request = mock(WebRequest::class.java)

    @AfterEach
    fun clearMdc() = MDC.clear()

    @Test
    fun `catch-all handler does not leak the raw exception message to the client`() {
        val secret = """ERROR: relation "users" does not exist; SELECT secret FROM users"""
        MDC.put(MDC_REQUEST_ID, "corr-123")

        val response = handler.handleGlobalException(RuntimeException(secret), request)
        val body = response.body!!

        assertEquals(500, response.statusCode.value())
        assertNotEquals(secret, body.message)
        assertFalse(body.message.orEmpty().contains("SELECT"), "message must not echo the raw SQL")
        assertFalse(body.details.orEmpty().contains("users"), "details must not echo schema names")
        assertEquals("corr-123", body.details, "details should carry the opaque correlation id")
    }

    @Test
    fun `illegal-state handler does not surface the raw exception message as the client message`() {
        val secret = "/var/lib/centsible/attachments/3f2c/secret.png: write failed"
        MDC.put(MDC_REQUEST_ID, "corr-456")

        val response = handler.handleIllegalState(IllegalStateException(secret), request)
        val body = response.body!!

        assertEquals(500, response.statusCode.value())
        assertNotEquals(secret, body.message)
        assertFalse(body.message.orEmpty().contains("/var/lib"), "message must not leak filesystem paths")
        assertFalse(body.details.orEmpty().contains("/var/lib"), "details must not leak filesystem paths")
        assertEquals("corr-456", body.details)
    }
}
