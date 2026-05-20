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
        val claimed = log.claimOrLog("Failed to claim next export job") {
            jobRepository.claimNextPending(workerProperties.id, workerProperties.leaseTimeoutSeconds)
        } ?: return
        process(claimed)
    }

    private fun process(claimed: ClaimedExportJob) {
        val job = claimed.job
        log.info("Rendering export job {} (type={}, attempt={})", job.id, job.type, job.attemptCount)
        try {
            val request = ExportRequest.parseFrom(claimed.payload)
            val format = request.format.toModelFormat()
            val rendered = renderers.find(job.type, format).render(request)
            jobRepository.markCompleted(job.id, rendered.pdf, rendered.filename)
            log.info("Completed export job {} (format={}, {} bytes)", job.id, format, rendered.pdf.size)
        } catch (ex: Exception) {
            log.error("Failed to render export job {}", job.id, ex)
            jobRepository.markFailed(job.id, ex.failureReason())
        }
    }
}

private fun ProtoExportFormat.toModelFormat(): ExportFormat = when (this) {
    ProtoExportFormat.CSV -> ExportFormat.CSV
    ProtoExportFormat.JSON -> ExportFormat.JSON
    // PDF (the proto default) and any future-unknown value fall back to PDF for back-compat
    // with payloads written before the format field existed.
    else -> ExportFormat.PDF
}
