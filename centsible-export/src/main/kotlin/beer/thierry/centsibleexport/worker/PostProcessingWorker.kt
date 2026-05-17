package beer.thierry.centsibleexport.worker

import beer.thierry.centsible.api.repository.IExportJobRepository
import beer.thierry.centsible.api.repository.IExportPostProcessingRepository
import beer.thierry.centsibleexport.postprocess.PostProcessorRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PostProcessingWorker(
    private val ppRepository: IExportPostProcessingRepository,
    private val jobRepository: IExportJobRepository,
    private val processors: PostProcessorRegistry,
    private val workerProperties: WorkerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${export.worker.poll-interval-ms:2000}")
    fun pollOnce() {
        val claimed = try {
            ppRepository.claimNextPending(workerProperties.id, workerProperties.leaseTimeoutSeconds)
        } catch (ex: Exception) {
            log.error("Failed to claim next post-processing row", ex)
            return
        } ?: return

        val job = jobRepository.fetchByIdForWorker(claimed.exportJobId)
        val pdf = jobRepository.fetchPdfForWorker(claimed.exportJobId)
        if (job == null || pdf == null) {
            ppRepository.markFailed(claimed.id, "Parent job ${claimed.exportJobId} missing PDF or row")
            return
        }

        try {
            processors.forType(claimed.type).execute(job, pdf.bytes, pdf.filename, claimed.config)
            ppRepository.markCompleted(claimed.id)
            log.info("Completed post-processing {} ({}) for job {}", claimed.id, claimed.type, claimed.exportJobId)
        } catch (ex: Exception) {
            log.error("Post-processing {} failed for job {}", claimed.id, claimed.exportJobId, ex)
            ppRepository.markFailed(claimed.id, ex.message ?: ex::class.qualifiedName ?: "unknown error")
        }
    }
}
