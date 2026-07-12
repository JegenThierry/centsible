package beer.thierry.centsiblerest.logging

import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.spi.ILoggingEvent

/**
 * Replaces likely-secret substrings in formatted log messages with `***REDACTED***`.
 *
 * Patterns covered:
 *  - JSON-style key/value pairs: "access_token":"…", "refresh_token":"…", "client_secret":"…",
 *    "secret_key":"…", "secret_id":"…", "api_key":"…", "password":"…", "token":"…", "authorization":"Bearer …"
 *  - Querystring-style: access_token=…, refresh_token=…, client_secret=…
 *  - HTTP header values: Authorization: Bearer ABC… (case-insensitive)
 *
 * We do NOT touch the value of `state` (the OAuth state token) — it is HMAC-signed and not
 * sensitive on its own, and keeping it visible helps debug OAuth callback failures.
 */
class SecretRedactingConverter : MessageConverter() {
    override fun convert(event: ILoggingEvent): String =
        redact(super.convert(event))

    companion object {
        private const val REDACTED = "***REDACTED***"

        private val JSON_VALUE = Regex(
            """("(?:access_token|refresh_token|client_secret|secret_key|secret_id|api_key|password|token)"\s*:\s*")[^"]*(")""",
            RegexOption.IGNORE_CASE,
        )
        private val AUTH_HEADER_JSON = Regex(
            """("authorization"\s*:\s*"(?:Bearer\s+|Basic\s+)?)[^"]*(")""",
            RegexOption.IGNORE_CASE,
        )
        private val QUERY_PARAM = Regex(
            """\b(access_token|refresh_token|client_secret|secret_key|secret_id|api_key|password|token)=([^&\s"]+)""",
            RegexOption.IGNORE_CASE,
        )
        private val AUTH_HEADER_PLAIN = Regex(
            """\b(Authorization\s*:\s*(?:Bearer|Basic)\s+)\S+""",
            RegexOption.IGNORE_CASE,
        )

        fun redact(msg: String): String =
            msg
                .replace(JSON_VALUE) { "${it.groupValues[1]}$REDACTED${it.groupValues[2]}" }
                .replace(AUTH_HEADER_JSON) { "${it.groupValues[1]}$REDACTED${it.groupValues[2]}" }
                .replace(QUERY_PARAM) { "${it.groupValues[1]}=$REDACTED" }
                .replace(AUTH_HEADER_PLAIN) { "${it.groupValues[1]}$REDACTED" }
    }
}
