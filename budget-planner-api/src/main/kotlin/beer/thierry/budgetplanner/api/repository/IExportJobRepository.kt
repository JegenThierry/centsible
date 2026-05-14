package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.export.ClaimedExportJob
import beer.thierry.budgetplanner.api.model.export.ExportJobDTO
import beer.thierry.budgetplanner.api.model.export.ExportPdf
import beer.thierry.budgetplanner.api.model.export.ExportType
import beer.thierry.budgetplanner.api.model.export.PostProcessingType
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

    fun retrigger(jobId: UUID, userId: UUID): ExportJobDTO?

    fun delete(jobId: UUID, userId: UUID): Boolean

    fun claimNextPending(workerId: String, leaseTimeoutSeconds: Long): ClaimedExportJob?

    fun fetchByIdForWorker(jobId: UUID): ExportJobDTO?

    fun fetchPdfForWorker(jobId: UUID): ExportPdf?

    fun markCompleted(jobId: UUID, pdf: ByteArray, pdfFilename: String)

    fun markFailed(jobId: UUID, errorMessage: String)
}
