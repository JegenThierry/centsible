package beer.thierry.centsibleexport.render

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.export.proto.ExportRequest

/** Renders an [ExportRequest] into a [RenderedExport] for the [ExportType] it [supports]. */
interface ExportRenderer {
    fun supports(): ExportType

    /**
     * Output format this renderer produces. Defaults to PDF so existing renderers don't need to
     * be touched; new renderers (CSV, JSON, ...) override this.
     */
    fun supportedFormat(): ExportFormat = ExportFormat.PDF

    fun render(request: ExportRequest): RenderedExport
}
