package beer.thierry.budgetplannerrest.model

import java.time.OffsetDateTime

data class ErrorResponse(
    val message: String?,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val details: String? = null
)
