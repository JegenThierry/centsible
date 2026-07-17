package beer.thierry.centsible.integrations.support

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * Outbound HTTP timeouts shared by every provider integration, bound from `integrations.http.*`.
 * Single-sources the connect/read defaults (and the property keys) that each provider's
 * `@Configuration` would otherwise repeat as a pair of `@Value` parameters.
 *
 * A config opts in with `@EnableConfigurationProperties(HttpTimeoutProperties::class)` and builds
 * its client on [requestFactory]; see [timeoutRequestFactory] for why the timeouts matter.
 */
@ConfigurationProperties(prefix = "integrations.http")
data class HttpTimeoutProperties(
    val connectTimeoutMs: Long = 10_000,
    val readTimeoutMs: Long = 30_000,
) {
    /** A [ClientHttpRequestFactory] applying these timeouts, ready for `RestClient.builder()`. */
    fun requestFactory(): ClientHttpRequestFactory =
        timeoutRequestFactory(Duration.ofMillis(connectTimeoutMs), Duration.ofMillis(readTimeoutMs))

    /**
     * A [RestClient.Builder] pre-wired with [requestFactory] (and [baseUrl] when supplied), so a
     * provider `@Configuration` builds its client with `httpTimeouts.restClientBuilder(apiBase).build()`
     * instead of repeating the `requestFactory` plumbing.
     */
    fun restClientBuilder(baseUrl: String? = null): RestClient.Builder {
        val builder = RestClient.builder().requestFactory(requestFactory())
        return if (baseUrl != null) builder.baseUrl(baseUrl) else builder
    }
}
