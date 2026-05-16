package beer.thierry.centsible.api.model.export

import java.time.OffsetDateTime
import java.util.UUID

data class ExportPostProcessingDTO(
    val id: UUID,
    val exportJobId: UUID,
    val type: PostProcessingType,
    val status: PostProcessingStatus,
    val config: Map<String, Any?>,
    val errorMessage: String?,
    val attemptCount: Int,
    val completedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime,
)
