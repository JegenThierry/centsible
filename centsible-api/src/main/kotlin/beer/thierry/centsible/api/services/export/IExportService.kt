package beer.thierry.centsible.api.services.export

import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.ExportPdf
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.export.PostProcessingType
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

interface IExportService {
    /** Enqueues an export job carrying the protobuf [protoPayload] and its ordered post-processing steps for the worker to pick up. */
    fun create(
        user: UserDTO,
        type: ExportType,
        title: String,
        protoPayload: ByteArray,
        postProcessing: List<Pair<PostProcessingType, Map<String, Any?>>>,
    ): ExportJobDTO

    fun list(user: UserDTO, page: Int, size: Int): List<ExportJobDTO>

    fun get(user: UserDTO, jobId: UUID): ExportJobDTO?

    fun download(user: UserDTO, jobId: UUID): ExportPdf?

    /** Re-enqueues an existing job for a fresh render; returns the refreshed job, or null if absent or not owned by [user]. */
    fun retrigger(user: UserDTO, jobId: UUID): ExportJobDTO?

    fun delete(user: UserDTO, jobId: UUID): Boolean
}
