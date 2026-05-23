package beer.thierry.centsible.integrations.support

import org.springframework.web.client.RestClient

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
 */
inline fun <reified T : Any> RestClient.RequestHeadersSpec<*>.exchangeOrThrow(
    provider: String,
    context: String? = null,
): T = exchange { _, response ->
    if (!response.statusCode.is2xxSuccessful) {
        throw IntegrationApiException(
            provider = provider,
            status = response.statusCode,
            body = response.bodyTo(String::class.java),
            context = context,
        )
    }
    response.bodyTo(T::class.java) ?: throw IntegrationApiException(
        provider = provider,
        status = response.statusCode,
        body = "empty body",
        context = context,
    )
}!!
