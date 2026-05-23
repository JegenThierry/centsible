package beer.thierry.centsible.integrations.support

import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient

@PublishedApi
internal val exchangeLog = LoggerFactory.getLogger("beer.thierry.centsible.integrations.support.RestClient")

/**
 * Executes the request and either returns the deserialized body of type [T] or throws an
 * [IntegrationApiException]. Replaces the per-call boilerplate:
 *
 *   .exchange { _, response ->
 *       if (!response.statusCode.is2xxSuccessful) throw apiError(...)
 *       response.bodyTo(T::class.java) ?: throw RuntimeException("empty body")
 *   }
 *
 * The [context] is appended to error messages when the response body alone is not enough to
 * diagnose the failure (e.g. "agreement creation failed", "token refresh failed").
 *
 * Emits WARN (4xx) / ERROR (5xx) with a truncated body snippet on failure; DEBUG on 2xx.
 * Authorization headers and request bodies are never logged — that's the caller's responsibility
 * to keep secrets out of the [context] string.
 */
inline fun <reified T : Any> RestClient.RequestHeadersSpec<*>.exchangeOrThrow(
    provider: String,
    context: String? = null,
): T = exchange { _, response ->
    val status = response.statusCode
    if (!status.is2xxSuccessful) {
        val body = response.bodyTo(String::class.java)
        val snippet = body?.take(IntegrationApiException.MAX_BODY_SNIPPET).orEmpty()
        if (status.is5xxServerError) {
            exchangeLog.error(
                "Provider HTTP failure provider={} context={} status={} bodySnippet={}",
                provider, context, status.value(), snippet,
            )
        } else {
            exchangeLog.warn(
                "Provider HTTP error provider={} context={} status={} bodySnippet={}",
                provider, context, status.value(), snippet,
            )
        }
        throw IntegrationApiException(
            provider = provider,
            status = status,
            body = body,
            context = context,
        )
    }
    if (exchangeLog.isDebugEnabled) {
        exchangeLog.debug("Provider HTTP ok provider={} context={} status={}", provider, context, status.value())
    }
    response.bodyTo(T::class.java) ?: run {
        exchangeLog.warn("Provider HTTP returned empty body provider={} context={} status={}", provider, context, status.value())
        throw IntegrationApiException(
            provider = provider,
            status = status,
            body = "empty body",
            context = context,
        )
    }
}
