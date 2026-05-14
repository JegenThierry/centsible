package beer.thierry.budgetplannerexport.worker

import beer.thierry.budgetplanner.api.model.export.ClaimedExportJob
import beer.thierry.budgetplanner.api.repository.IExportJobRepository
import beer.thierry.budgetplanner.export.proto.ExportRequest
import beer.thierry.budgetplannerexport.render.RendererRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ExportJobWorker(
    private val jobRepository: IExportJobRepository,
    private val renderers: RendererRegistry,
    private val workerProperties: WorkerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${export.worker.poll-interval-ms:2000}")
    fun pollOnce() {
        val claimed = try {
            jobRepository.claimNextPending(workerProperties.id, workerProperties.leaseTimeoutSeconds)
        } catch (ex: Exception) {
            log.error("Failed to claim next export job", ex)
            return
        } ?: return

        process(claimed)
    }

    private fun process(claimed: ClaimedExportJob) {
        val job = claimed.job
        log.info("Rendering export job {} (type={}, attempt={})", job.id, job.type, job.attemptCount)
        try {
            val request = ExportRequest.parseFrom(claimed.payload)
            val rendered = renderers.forType(job.type).render(request)
            jobRepository.markCompleted(job.id, rendered.pdf, rendered.filename)
            log.info("Completed export job {} ({} bytes)", job.id, rendered.pdf.size)
        } catch (ex: Exception) {
            log.error("Failed to render export job {}", job.id, ex)
            jobRepository.markFailed(job.id, ex.message ?: ex::class.qualifiedName ?: "unknown error")
        }
    }
}
