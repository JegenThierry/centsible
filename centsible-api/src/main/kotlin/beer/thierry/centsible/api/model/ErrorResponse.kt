package beer.thierry.centsible.api.model

import java.time.OffsetDateTime

data class ErrorResponse(
    val message: String? = null,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val details: String? = null,
    val fieldErrors: Map<String, String>? = null
)
