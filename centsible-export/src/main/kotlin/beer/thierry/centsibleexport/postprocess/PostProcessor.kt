package beer.thierry.centsibleexport.postprocess

import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.PostProcessingType

interface PostProcessor {
    fun supports(): PostProcessingType
    fun execute(job: ExportJobDTO, pdf: ByteArray, pdfFilename: String, config: Map<String, Any?>)
}
