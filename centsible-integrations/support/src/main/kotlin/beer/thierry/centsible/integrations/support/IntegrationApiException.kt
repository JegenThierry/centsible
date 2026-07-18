package beer.thierry.centsible.integrations.support

import org.springframework.http.HttpStatusCode

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
