package beer.thierry.centsiblerest.logging

import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.spi.ILoggingEvent

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
