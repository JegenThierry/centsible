package beer.thierry.centsibleexport.render

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.export.proto.ExportRequest

interface ExportRenderer {
    fun supports(): ExportType
    fun render(request: ExportRequest): RenderedExport
}
