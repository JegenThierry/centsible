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
        log.debug("Post-processing worker poll tick worker={}", workerProperties.id)
        try {
            val claimed = log.claimOrLog("Failed to claim next post-processing row") {
                ppRepository.claimNextPending(workerProperties.id, workerProperties.leaseTimeoutSeconds)
            } ?: return

            // Same poison-row cap as ExportJobWorker: a step only accumulates attempts by killing the JVM
            // mid-execute (a send holds ~3x the PDF in heap while base64-encoding it), and without this
            // it is re-leased forever, crash-looping the worker and starving every other user's emails.
            if (claimed.attemptCount > workerProperties.maxAttempts) {
                log.error(
                    "Dead-lettering post-processing ppId={} jobId={} after {} attempts (max {})",
                    claimed.id, claimed.exportJobId, claimed.attemptCount, workerProperties.maxAttempts,
                )
                ppRepository.markFailed(
                    claimed.id,
                    workerProperties.id,
                    "Aborted after ${claimed.attemptCount} attempts; earlier attempts terminated the worker",
                )
                return
            }

            val job = jobRepository.fetchByIdForWorker(claimed.exportJobId)
            val pdf = jobRepository.fetchPdfForWorker(claimed.exportJobId)
            if (job == null || pdf == null) {
                log.warn(
                    "Skipping post-processing ppId={} jobId={} reason=missing-job-or-pdf",
                    claimed.id, claimed.exportJobId,
                )
                ppRepository.markFailed(
                    claimed.id,
                    workerProperties.id,
                    "Parent job ${claimed.exportJobId} missing PDF or row",
                )
                return
            }

            val startNanos = System.nanoTime()
            log.info(
                "Leased post-processing ppId={} type={} jobId={} attempt={}",
                claimed.id, claimed.type, claimed.exportJobId, claimed.attemptCount,
            )
            try {
                processors.forType(claimed.type).execute(job, pdf.bytes, pdf.filename, claimed.config)
                ppRepository.markCompleted(claimed.id, workerProperties.id)
                val elapsedMs = elapsedMsSince(startNanos)
                log.info(
                    "Completed post-processing ppId={} type={} jobId={} elapsedMs={}",
                    claimed.id, claimed.type, claimed.exportJobId, elapsedMs,
                )
            } catch (ex: Exception) {
                val elapsedMs = elapsedMsSince(startNanos)
                log.error(
                    "Failed post-processing ppId={} type={} jobId={} elapsedMs={}",
                    claimed.id, claimed.type, claimed.exportJobId, elapsedMs, ex,
                )
                ppRepository.markFailed(claimed.id, workerProperties.id, ex.failureReason())
            }
        } catch (ex: Exception) {
            log.error("Unhandled error in post-processing worker poll loop worker={}", workerProperties.id, ex)
        }
    }
}
