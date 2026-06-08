package beer.thierry.centsible.integrations.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.ResourceAccessException
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class ProviderRetryTest {

    @Test
    fun `successful call runs the block exactly once`() {
        val retry = providerRetry("test-success")
        val invocations = AtomicInteger(0)

        val result = retry.call {
            invocations.incrementAndGet()
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(1, invocations.get())
    }

    @Test
    fun `retries transient IOException up to the configured max attempts`() {
        val retry = providerRetry("test-io")
        val invocations = AtomicInteger(0)

        val ex = assertThrows(IOException::class.java) {
            retry.call<Unit> {
                invocations.incrementAndGet()
                throw IOException("connection reset")
            }
        }

        assertEquals(3, invocations.get(), "expected 3 attempts before giving up")
        assertEquals("connection reset", ex.message)
    }

    @Test
    fun `retries transient ResourceAccessException`() {
        val retry = providerRetry("test-rae")
        val invocations = AtomicInteger(0)

        assertThrows(ResourceAccessException::class.java) {
            retry.call<Unit> {
                invocations.incrementAndGet()
                throw ResourceAccessException("network down")
            }
        }

        assertEquals(3, invocations.get())
    }

    @Test
    fun `retries 5xx IntegrationApiException`() {
        val retry = providerRetry("test-5xx")
        val invocations = AtomicInteger(0)

        assertThrows(IntegrationApiException::class.java) {
            retry.call<Unit> {
                invocations.incrementAndGet()
                throw IntegrationApiException(
                    provider = "X",
                    status = HttpStatus.BAD_GATEWAY,
                    body = "down",
                )
            }
        }

        assertEquals(3, invocations.get())
    }

    @Test
    fun `retries 429 IntegrationApiException`() {
        val retry = providerRetry("test-429")
        val invocations = AtomicInteger(0)

        assertThrows(IntegrationApiException::class.java) {
            retry.call<Unit> {
                invocations.incrementAndGet()
                throw IntegrationApiException(
                    provider = "X",
                    status = HttpStatus.TOO_MANY_REQUESTS,
                    body = "slow down",
                )
            }
        }

        assertEquals(3, invocations.get())
    }

    @Test
    fun `does not retry 4xx IntegrationApiException other than 429`() {
        val retry = providerRetry("test-4xx")
        val invocations = AtomicInteger(0)

        assertThrows(IntegrationApiException::class.java) {
            retry.call<Unit> {
                invocations.incrementAndGet()
                throw IntegrationApiException(
                    provider = "X",
                    status = HttpStatus.UNAUTHORIZED,
                    body = "expired token",
                )
            }
        }

        assertEquals(1, invocations.get(), "4xx (non-429) must not be retried")
    }

    @Test
    fun `does not retry unrelated runtime exceptions`() {
        val retry = providerRetry("test-runtime")
        val invocations = AtomicInteger(0)

        assertThrows(IllegalStateException::class.java) {
            retry.call<Unit> {
                invocations.incrementAndGet()
                throw IllegalStateException("bug")
            }
        }

        assertEquals(1, invocations.get())
    }

    @Test
    fun `succeeds on the second attempt after one transient failure`() {
        val retry = providerRetry("test-recovery")
        val invocations = AtomicInteger(0)

        val result = retry.call {
            val n = invocations.incrementAndGet()
            if (n == 1) throw IOException("first failure")
            "recovered"
        }

        assertEquals("recovered", result)
        assertEquals(2, invocations.get())
        assertTrue(invocations.get() == 2)
    }
}
