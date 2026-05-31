package beer.thierry.centsible.integrations.support

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.net.ServerSocket
import java.time.Duration
import kotlin.system.measureTimeMillis
import kotlin.test.assertFailsWith

class HttpTimeoutsTest {

    /**
     * The regression this finding is about: with no read timeout a connection to an upstream that
     * accepts but never responds blocks the calling thread forever. A factory from
     * [timeoutRequestFactory] must instead abort with a [ResourceAccessException] (wrapping a
     * SocketTimeoutException) once the read timeout elapses.
     */
    @Test
    fun `read timeout aborts a hung response instead of blocking forever`() {
        ServerSocket(0).use { server ->
            // Accept the connection on a daemon thread but never write a response, simulating a
            // hung upstream. The socket is held open until the JVM exits.
            Thread {
                runCatching {
                    val socket = server.accept()
                    Thread.sleep(60_000)
                    socket.close()
                }
            }.apply { isDaemon = true }.start()

            val client = RestClient.builder()
                .requestFactory(timeoutRequestFactory(Duration.ofSeconds(2), Duration.ofMillis(300)))
                .baseUrl("http://127.0.0.1:${server.localPort}")
                .build()

            val elapsed = measureTimeMillis {
                assertFailsWith<ResourceAccessException> {
                    client.get().uri("/").retrieve().body(String::class.java)
                }
            }

            // 300 ms read timeout + slack; must be far below the 60 s server hold, proving it
            // aborted on the timeout rather than waiting for (a never-arriving) response.
            assertTrue(elapsed < 5_000, "call should abort on read timeout, took ${elapsed}ms")
        }
    }
}
