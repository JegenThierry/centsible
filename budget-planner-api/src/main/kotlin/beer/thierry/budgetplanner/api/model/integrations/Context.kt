package beer.thierry.budgetplanner.api.model.integrations

import java.util.UUID

data class ProviderContext(
    val userId: UUID,
    val connectionId: UUID,
    val displayName: String,
    val config: Map<String, Any?>,
    val credentials: Map<String, String>,
    val lastCursor: String? = null,
)
