package beer.thierry.centsible.integrations.support

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.client.ClientHttpRequestFactory
import java.time.Duration

@ConfigurationProperties(prefix = "integrations.http")
data class HttpTimeoutProperties(
    val connectTimeoutMs: Long = 10_000,
    val readTimeoutMs: Long = 30_000,
) {
    fun requestFactory(): ClientHttpRequestFactory =
        timeoutRequestFactory(Duration.ofMillis(connectTimeoutMs), Duration.ofMillis(readTimeoutMs))
}
