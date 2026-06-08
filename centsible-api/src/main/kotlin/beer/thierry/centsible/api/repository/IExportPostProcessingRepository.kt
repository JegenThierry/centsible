package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.export.ExportPostProcessingDTO
import java.util.UUID

interface IExportPostProcessingRepository {

    fun fetchByJob(jobId: UUID): List<ExportPostProcessingDTO>

    /**
     * Atomically leases the oldest pending post-processing step whose parent job has completed (or one whose
     * lease lapsed past [leaseTimeoutSeconds]) to [workerId], marking it in-progress; null if none is claimable.
     */
    fun claimNextPending(workerId: String, leaseTimeoutSeconds: Long): ExportPostProcessingDTO?

    fun markCompleted(id: UUID)

    fun markFailed(id: UUID, errorMessage: String)
}
