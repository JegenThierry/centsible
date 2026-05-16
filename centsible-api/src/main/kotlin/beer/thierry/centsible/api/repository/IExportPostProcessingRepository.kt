package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.export.ExportPostProcessingDTO
import java.util.UUID

interface IExportPostProcessingRepository {

    fun fetchByJob(jobId: UUID): List<ExportPostProcessingDTO>

    fun claimNextPending(workerId: String, leaseTimeoutSeconds: Long): ExportPostProcessingDTO?

    fun markCompleted(id: UUID)

    fun markFailed(id: UUID, errorMessage: String)
}
