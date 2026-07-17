package beer.thierry.centsibleexport.worker

import beer.thierry.centsible.api.model.export.ClaimedExportJob
import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.api.repository.IExportJobRepository
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.RendererRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import beer.thierry.centsible.export.proto.ExportFormat as ProtoExportFormat

@Component
class ExportJobWorker(
    private val jobRepository: IExportJobRepository,
    private val renderers: RendererRegistry,
    private val workerProperties: WorkerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${export.worker.poll-interval-ms:2000}")
    fun pollOnce() {
        log.debug("Export worker poll tick worker={}", workerProperties.id)
        try {
            val claimed = log.claimOrLog("Failed to claim next export job") {
                jobRepository.claimNextPending(workerProperties.id, workerProperties.leaseTimeoutSeconds)
            } ?: return
            process(claimed)
        } catch (ex: Exception) {
            log.error("Unhandled error in export worker poll loop worker={}", workerProperties.id, ex)
        }
    }

    private fun process(claimed: ClaimedExportJob) {
        val job = claimed.job
        val startNanos = System.nanoTime()
        log.info("Leased export job jobId={} type={} attempt={}", job.id, job.type, job.attemptCount)
        if (job.attemptCount > workerProperties.maxAttempts) {
            log.error(
                "Dead-lettering export job jobId={} after {} attempts (max {})",
                job.id, job.attemptCount, workerProperties.maxAttempts,
            )
            jobRepository.markFailed(
                job.id,
                workerProperties.id,
                "Aborted after ${job.attemptCount} attempts; earlier attempts terminated the worker",
            )
            return
        }
        try {
            val request = ExportRequest.parseFrom(claimed.payload)
            val format = request.format.toModelFormat()
            val rendered = renderers.find(job.type, format).render(request)
            jobRepository.markCompleted(job.id, workerProperties.id, rendered.pdf, rendered.filename)
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
            log.info(
                "Completed export job jobId={} format={} bytes={} elapsedMs={}",
                job.id, format, rendered.pdf.size, elapsedMs,
            )
        } catch (ex: Exception) {
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
            log.error("Failed export job jobId={} elapsedMs={}", job.id, elapsedMs, ex)
            jobRepository.markFailed(job.id, workerProperties.id, ex.failureReason())
        }
    }
}

private fun ProtoExportFormat.toModelFormat(): ExportFormat = when (this) {
    ProtoExportFormat.CSV -> ExportFormat.CSV
    ProtoExportFormat.JSON -> ExportFormat.JSON
    else -> ExportFormat.PDF
}
