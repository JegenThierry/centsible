package beer.thierry.centsible.api.model.export

import java.time.OffsetDateTime
import java.util.UUID

data class ExportJobDTO(
    val id: UUID,
    val userId: UUID,
    val type: ExportType,
    val status: ExportStatus,
    val title: String,
    val pdfFilename: String?,
    val errorMessage: String?,
    val attemptCount: Int,
    val completedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime,
    val postProcessing: List<ExportPostProcessingDTO> = emptyList(),
)

/** A pending job leased by a worker, paired with its protobuf export-request [payload]. */
data class ClaimedExportJob(
    val job: ExportJobDTO,
    val payload: ByteArray,
)

data class ExportPdf(
    val bytes: ByteArray,
    val filename: String,
)
