package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.export.ClaimedExportJob
import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.ExportPdf
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.export.PostProcessingType
import java.util.UUID

interface IExportJobRepository {

    fun enqueue(
        userId: UUID,
        type: ExportType,
        title: String,
        payload: ByteArray,
        postProcessing: List<Pair<PostProcessingType, Map<String, Any?>>>,
    ): ExportJobDTO

    fun fetchByUser(userId: UUID, page: Int, pageSize: Int): List<ExportJobDTO>

    fun fetchById(jobId: UUID, userId: UUID): ExportJobDTO?

    fun fetchPdf(jobId: UUID, userId: UUID): ExportPdf?

    /**
     * Resets the owned job (and its post-processing rows) back to pending, clearing prior PDF, error and lease.
     * Only a settled (completed or failed) job may be retriggered — null otherwise, so a job a worker is
     * mid-render on keeps its lease.
     */
    fun retrigger(jobId: UUID, userId: UUID): ExportJobDTO?

    fun delete(jobId: UUID, userId: UUID): Boolean

    /**
     * Atomically leases the oldest pending job (or one whose lease lapsed past [leaseTimeoutSeconds]) to
     * [workerId], marking it in-progress and bumping its attempt count; null if none is claimable.
     */
    fun claimNextPending(workerId: String, leaseTimeoutSeconds: Long): ClaimedExportJob?

    fun fetchByIdForWorker(jobId: UUID): ExportJobDTO?

    fun fetchPdfForWorker(jobId: UUID): ExportPdf?

    /**
     * Settles the job only while [workerId] still holds its lease; a no-op once the lease lapsed and
     * another worker (or a retrigger) took the job over.
     */
    fun markCompleted(jobId: UUID, workerId: String, pdf: ByteArray, pdfFilename: String)

    /** Fails the job only while [workerId] still holds its lease. See [markCompleted]. */
    fun markFailed(jobId: UUID, workerId: String, errorMessage: String)
}
