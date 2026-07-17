package beer.thierry.centsible.api.model.integrations

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
    @field:NotBlank(message = "{validation.connection.providerKey.required}")
    @field:Size(max = 100, message = "{validation.connection.providerKey.tooLong}")
    var providerKey: String = "",

    @field:NotBlank(message = "{validation.connection.displayName.required}")
    @field:Size(max = 200, message = "{validation.connection.displayName.tooLong}")
    var displayName: String = "",

    var values: Map<String, Any?> = emptyMap(),
)
