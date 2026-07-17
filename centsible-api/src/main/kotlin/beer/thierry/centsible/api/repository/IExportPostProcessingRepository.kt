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

    /**
     * Settles the step only while [workerId] still holds its lease; a no-op once the lease lapsed and
     * another worker took the step over, so a slow send cannot overwrite the outcome of the retry.
     */
    fun markCompleted(id: UUID, workerId: String)

    /** Fails the step only while [workerId] still holds its lease. See [markCompleted]. */
    fun markFailed(id: UUID, workerId: String, errorMessage: String)
}
