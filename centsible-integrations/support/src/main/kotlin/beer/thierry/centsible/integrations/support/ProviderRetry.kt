package beer.thierry.centsible.integrations.support

import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.slf4j.LoggerFactory
import org.springframework.web.client.ResourceAccessException
import java.io.IOException
import java.time.Duration

private val log = LoggerFactory.getLogger(ProviderRetry::class.java)

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
    val retry = Retry.of("$provider-http", config)
    retry.eventPublisher.onRetry { event ->
        log.warn(
            "Retrying provider call provider={} attempt={} lastException={}",
            provider,
            event.numberOfRetryAttempts,
            event.lastThrowable?.javaClass?.simpleName,
        )
    }
    return ProviderRetry(retry)
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
