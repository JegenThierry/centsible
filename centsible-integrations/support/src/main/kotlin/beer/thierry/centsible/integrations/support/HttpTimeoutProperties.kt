package beer.thierry.centsible.integrations.support

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@ConfigurationProperties(prefix = "integrations.http")
data class HttpTimeoutProperties(
    val connectTimeoutMs: Long = 10_000,
    val readTimeoutMs: Long = 30_000,
) {
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
