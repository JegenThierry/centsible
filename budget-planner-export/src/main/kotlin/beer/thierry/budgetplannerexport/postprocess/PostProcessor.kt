package beer.thierry.budgetplannerexport.postprocess

import beer.thierry.budgetplanner.api.model.export.ExportJobDTO
import beer.thierry.budgetplanner.api.model.export.PostProcessingType

interface PostProcessor {
    fun supports(): PostProcessingType
    fun execute(job: ExportJobDTO, pdf: ByteArray, pdfFilename: String, config: Map<String, Any?>)
}
