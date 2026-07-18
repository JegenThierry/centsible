package beer.thierry.centsiblerest.exceptions

import beer.thierry.centsible.api.model.ErrorResponse
import beer.thierry.centsiblerest.logging.MDC_REQUEST_ID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.slf4j.MDC
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.util.unit.DataSize
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler(StaticMessageSource(), DataSize.ofMegabytes(11))

    private val request = mock(WebRequest::class.java).also {
        `when`(it.getDescription(false)).thenReturn("uri=/api/test")
    }

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
    fun `illegal-state routed to the catch-all does not surface the raw exception message as the client message`() {
        val secret = "/var/lib/centsible/attachments/3f2c/secret.png: write failed"
        MDC.put(MDC_REQUEST_ID, "corr-456")

        val response = handler.handleGlobalException(IllegalStateException(secret), request)
        val body = response.body!!

        assertEquals(500, response.statusCode.value())
        assertNotEquals(secret, body.message)
        assertFalse(body.message.orEmpty().contains("/var/lib"), "message must not leak filesystem paths")
        assertFalse(body.details.orEmpty().contains("/var/lib"), "details must not leak filesystem paths")
        assertEquals("corr-456", body.details)
    }

    @Test
    fun `type-mismatch handler does not leak the converter's internal class names to the client`() {
        val cause = IllegalArgumentException(
            "No enum constant beer.thierry.centsible.api.model.category.CategoryType.BOGUS"
        )
        val ex = MethodArgumentTypeMismatchException(
            "BOGUS", CategoryTypeStandIn::class.java, "type", mock(MethodParameter::class.java), cause,
        )

        val response = handler.handleTypeMismatch(ex, request)
        val fieldError = response.body!!.fieldErrors!!.getValue("type")

        assertEquals(400, response.statusCode.value())
        assertFalse(fieldError.contains("beer.thierry"), "field error must not echo internal package names")
        assertFalse(fieldError.contains("No enum constant"), "field error must not echo the converter message")
        assertEquals("validation.generic.invalid", fieldError, "field error should be a bundle key")
    }

    /**
     * Registering the advice is what detects an @ExceptionHandler mapped twice across the class and
     * its ResponseEntityExceptionHandler parent — Spring fails the context at startup, not per call.
     */
    @Test
    fun `exception handler methods are unambiguous and route to the most specific handler`() {
        val resolver = ExceptionHandlerMethodResolver(GlobalExceptionHandler::class.java)

        assertEquals(
            "handleTypeMismatch",
            resolver.resolveMethodByExceptionType(MethodArgumentTypeMismatchException::class.java)?.name,
            "the app's handler must win over the parent's broader TypeMismatchException mapping",
        )
        assertEquals(
            "handleGlobalException",
            resolver.resolveMethodByExceptionType(RuntimeException::class.java)?.name,
            "anything the framework does not claim still falls to the catch-all",
        )
        assertEquals(
            "handleException",
            resolver.resolveMethodByExceptionType(NoResourceFoundException::class.java)?.name,
            "framework exceptions must route through the parent instead of the catch-all",
        )
    }

    @Test
    fun `missing request parameter is a 400 in the app's error shape, not a 500`() {
        MDC.put(MDC_REQUEST_ID, "corr-789")
        val ex = MissingServletRequestParameterException("amount", "BigDecimal")

        val response = handler.handleException(ex, request)!!
        val body = assertInstanceOf(ErrorResponse::class.java, response.body)

        assertEquals(400, response.statusCode.value())
        assertEquals("error.request.invalid", body.message)
        assertEquals("corr-789", body.details, "details should carry the opaque correlation id")
    }

    @Test
    fun `unsupported method is a 405 that still advertises Allow`() {
        val ex = HttpRequestMethodNotSupportedException("GET", listOf("POST"))

        val response = handler.handleException(ex, request)!!

        assertEquals(405, response.statusCode.value())
        assertInstanceOf(ErrorResponse::class.java, response.body)
        assertTrue(response.headers.allow.contains(HttpMethod.POST), "405 must keep the framework's Allow header")
    }

    @Test
    fun `unknown url is a 404 in the app's error shape`() {
        val ex = NoResourceFoundException(HttpMethod.GET, "/api/nope", "No static resource")

        val response = handler.handleException(ex, request)!!
        val body = assertInstanceOf(ErrorResponse::class.java, response.body)

        assertEquals(404, response.statusCode.value())
        assertEquals("error.request.notFound", body.message)
    }

    @Test
    fun `unusable async request is not answered at all`() {
        val ex = org.springframework.web.context.request.async.AsyncRequestNotUsableException("client gone")

        assertNotNull(handler)
        assertEquals(null, handler.handleException(ex, request), "the response is already unusable; send nothing")
    }

    private enum class CategoryTypeStandIn { INCOME }
}
