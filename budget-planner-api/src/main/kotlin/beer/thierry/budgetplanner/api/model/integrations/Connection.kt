package beer.thierry.budgetplanner.api.model.integrations

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

enum class ProviderConnectionStatus { NEW, ACTIVE, ERROR, REVOKED }

data class ProviderConnectionDTO(
    val id: UUID,
    val providerKey: String,
    val displayName: String,
    val status: ProviderConnectionStatus,
    val config: Map<String, Any?> = emptyMap(),
    val lastSyncAt: OffsetDateTime? = null,
    val lastError: String? = null,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime,
)

data class ProviderConnectionForm(
    @field:NotBlank(message = "Provider key is required.")
    @field:Size(max = 100)
    var providerKey: String = "",

    @field:NotBlank(message = "Display name is required.")
    @field:Size(max = 200)
    var displayName: String = "",

    var values: Map<String, Any?> = emptyMap(),
)
