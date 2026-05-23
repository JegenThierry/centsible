package beer.thierry.centsible.integrations.support

import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.web.client.ResourceAccessException
import java.io.IOException
import java.time.Duration

/**
 * Thin façade over a resilience4j [Retry] tailored for outbound provider HTTP calls.
 *
 * Wrapping the resilience4j type means downstream integration modules can call `.call { ... }`
 * without dragging resilience4j onto their compile classpath, and lets the policy be tuned in
 * exactly one place.
 *
 * Retries only on transient conditions (IOException, ResourceAccessException, HTTP 429, HTTP
 * 5xx); 4xx responses surface immediately because they signal misconfiguration or expired
 * credentials, neither of which benefits from a retry.
 *
 * Defaults: 3 attempts, exponential backoff 500 ms → 1 s → 2 s (capped at 5 s).
 */
class ProviderRetry internal constructor(private val delegate: Retry) {
    fun <T> call(block: () -> T): T = delegate.executeCallable(block)
}

fun providerRetry(provider: String): ProviderRetry {
    val config = RetryConfig.custom<Any>()
        .maxAttempts(MAX_ATTEMPTS)
        .intervalFunction(
            IntervalFunction.ofExponentialBackoff(
                Duration.ofMillis(INITIAL_BACKOFF_MS),
                BACKOFF_MULTIPLIER,
                Duration.ofMillis(MAX_BACKOFF_MS),
            )
        )
        .retryOnException(::isTransientHttpFailure)
        .build()
    return ProviderRetry(Retry.of("$provider-http", config))
}

private fun isTransientHttpFailure(e: Throwable): Boolean = when (e) {
    is IOException -> true
    is ResourceAccessException -> true
    is IntegrationApiException -> e.status.value() == 429 || e.status.value() in 500..599
    else -> false
}

private const val MAX_ATTEMPTS = 3
private const val INITIAL_BACKOFF_MS = 500L
private const val BACKOFF_MULTIPLIER = 2.0
private const val MAX_BACKOFF_MS = 5_000L
