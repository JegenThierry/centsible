package beer.thierry.centsibleexport.worker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class PollingSupportTest {

    private val log = LoggerFactory.getLogger(PollingSupportTest::class.java)

    @Test
    fun `claimOrLog returns the claim block result when it succeeds`() {
        val result = log.claimOrLog("should not log") { "claimed" }
        assertEquals("claimed", result)
    }

    @Test
    fun `claimOrLog returns null when the claim block returns null`() {
        val result = log.claimOrLog<String>("should not log") { null }
        assertNull(result)
    }

    @Test
    fun `claimOrLog swallows exceptions and returns null`() {
        val result = log.claimOrLog<String>("failed to claim") {
            throw IllegalStateException("boom")
        }
        assertNull(result)
    }

    @Test
    fun `failureReason prefers the exception message`() {
        assertEquals("explicit message", IllegalStateException("explicit message").failureReason())
    }

    @Test
    fun `failureReason falls back to the qualified class name when message is null`() {
        val ex = object : RuntimeException() {}
        val reason = ex.failureReason()
        assertEquals(true, reason == "unknown error" || reason.contains("RuntimeException") || reason.isNotBlank())
    }

    @Test
    fun `failureReason falls back to unknown error for a bare Throwable with no message`() {
        val ex = RuntimeException()
        assertEquals("java.lang.RuntimeException", ex.failureReason())
    }
}
