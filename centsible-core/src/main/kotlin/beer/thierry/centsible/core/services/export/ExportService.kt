package beer.thierry.centsible.core.services.export

import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.ExportPdf
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.export.PostProcessingType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IExportJobRepository
import beer.thierry.centsible.api.services.export.IExportService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ExportService(
    private val exportJobRepository: IExportJobRepository,
) : IExportService {

    private val log = LoggerFactory.getLogger(ExportService::class.java)

    override fun create(
        user: UserDTO,
        type: ExportType,
        title: String,
        protoPayload: ByteArray,
        postProcessing: List<Pair<PostProcessingType, Map<String, Any?>>>,
    ): ExportJobDTO {
        val job = exportJobRepository.enqueue(user.id, type, title, protoPayload, postProcessing)
        log.info(
            "Enqueued export job id={} userId={} type={} payloadBytes={} postProcessing={}",
            job.id, user.id, type, protoPayload.size, postProcessing.map { it.first },
        )
        return job
    }

    override fun list(user: UserDTO, page: Int, size: Int): List<ExportJobDTO> =
        exportJobRepository.fetchByUser(user.id, page, size)

    override fun get(user: UserDTO, jobId: UUID): ExportJobDTO? =
        exportJobRepository.fetchById(jobId, user.id)

    override fun download(user: UserDTO, jobId: UUID): ExportPdf? =
        exportJobRepository.fetchPdf(jobId, user.id)

    override fun retrigger(user: UserDTO, jobId: UUID): ExportJobDTO? {
        val job = exportJobRepository.retrigger(jobId, user.id)
        if (job != null) {
            log.info("Retriggered export job id={} userId={}", jobId, user.id)
        }
        return job
    }

    override fun delete(user: UserDTO, jobId: UUID): Boolean {
        val deleted = exportJobRepository.delete(jobId, user.id)
        if (deleted) {
            log.info("Deleted export job id={} userId={}", jobId, user.id)
        }
        return deleted
    }
}
