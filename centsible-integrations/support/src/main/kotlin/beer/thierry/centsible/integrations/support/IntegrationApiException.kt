package beer.thierry.centsible.integrations.support

import org.springframework.http.HttpStatusCode

/**
 * Single error type for every outbound HTTP call from an integration module.
 *
 * The truncated `bodySnippet` is here so structured logging can record it server-side; it is
 * intentionally NOT echoed into user-facing URLs by the OAuth callback flow (see
 * IntegrationsResource.buildReturnUrl) because PSD2 / payments providers regularly include
 * IBAN fragments, institution identifiers, and other PII in error bodies.
 */
class IntegrationApiException(
    val provider: String,
    val status: HttpStatusCode,
    body: String?,
    val context: String? = null,
) : RuntimeException(buildMessage(provider, status, body, context)) {

    val bodySnippet: String = body?.take(MAX_BODY_SNIPPET) ?: ""

    companion object {
        const val MAX_BODY_SNIPPET = 500

        private fun buildMessage(provider: String, status: HttpStatusCode, body: String?, context: String?): String {
            val snippet = body?.take(MAX_BODY_SNIPPET).orEmpty()
            val ctx = context?.let { " ($it)" }.orEmpty()
            return "$provider API error: $status$ctx $snippet"
        }
    }
}
