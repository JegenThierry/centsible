package beer.thierry.budgetplannerexport.render

import beer.thierry.budgetplanner.api.model.export.ExportType
import beer.thierry.budgetplanner.export.proto.ExportRequest

interface ExportRenderer {
    fun supports(): ExportType
    fun render(request: ExportRequest): RenderedExport
}
