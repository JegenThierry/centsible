package beer.thierry.centsible.integrations.support

import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.time.Duration

/**
 * Builds a [ClientHttpRequestFactory] with explicit connect and read timeouts.
 *
 * Spring's default request factory leaves both timeouts unset, so a connection to a hung or slow
 * upstream blocks the calling thread indefinitely. Combined with [providerRetry] (which retries
 * transient failures) and the single-threaded scheduler, one stalled provider call can wedge all
 * scheduled work. Every outbound [org.springframework.web.client.RestClient] /
 * [org.springframework.web.client.RestTemplate] should therefore be built on a factory from here.
 *
 * @param connectTimeout max time to establish the TCP connection.
 * @param readTimeout max time to wait for response bytes once connected.
 */
fun timeoutRequestFactory(
    connectTimeout: Duration,
    readTimeout: Duration,
): ClientHttpRequestFactory = SimpleClientHttpRequestFactory().apply {
    setConnectTimeout(connectTimeout)
    setReadTimeout(readTimeout)
}
