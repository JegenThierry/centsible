package beer.thierry.centsible.core.services.export

import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.ExportPdf
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.export.PostProcessingType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IExportJobRepository
import beer.thierry.centsible.api.services.export.IExportService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ExportService(
    private val exportJobRepository: IExportJobRepository,
) : IExportService {

    override fun create(
        user: UserDTO,
        type: ExportType,
        title: String,
        protoPayload: ByteArray,
        postProcessing: List<Pair<PostProcessingType, Map<String, Any?>>>,
    ): ExportJobDTO = exportJobRepository.enqueue(user.id, type, title, protoPayload, postProcessing)

    override fun list(user: UserDTO, page: Int, size: Int): List<ExportJobDTO> =
        exportJobRepository.fetchByUser(user.id, page, size)

    override fun get(user: UserDTO, jobId: UUID): ExportJobDTO? =
        exportJobRepository.fetchById(jobId, user.id)

    override fun download(user: UserDTO, jobId: UUID): ExportPdf? =
        exportJobRepository.fetchPdf(jobId, user.id)

    override fun retrigger(user: UserDTO, jobId: UUID): ExportJobDTO? =
        exportJobRepository.retrigger(jobId, user.id)

    override fun delete(user: UserDTO, jobId: UUID): Boolean =
        exportJobRepository.delete(jobId, user.id)
}
