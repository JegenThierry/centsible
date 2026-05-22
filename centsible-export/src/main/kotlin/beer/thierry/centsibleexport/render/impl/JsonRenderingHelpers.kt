package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.ExportRenderer
import beer.thierry.centsibleexport.render.RenderedExport
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Base class that wires a JSON renderer into the [ExportRenderer] SPI. Subclasses build a
 * structured envelope (typically `envelopeHeader(...) + ("data" to <something>)`); this class
 * serializes it via Jackson with pretty-printing and packages the bytes.
 */
abstract class JsonExportRenderer(
    private val objectMapper: ObjectMapper,
) : ExportRenderer {

    final override fun supportedFormat(): ExportFormat = ExportFormat.JSON

    final override fun render(request: ExportRequest): RenderedExport {
        val envelope = buildJson(request)
        return RenderedExport(
            pdf = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(envelope),
            filename = "${filenameStem(request)}-${filenameTimestamp()}.json",
        )
    }

    protected abstract fun buildJson(request: ExportRequest): Any
    protected abstract fun filenameStem(request: ExportRequest): String

    /** Shared header so every JSON envelope identifies itself the same way. */
    protected fun envelopeHeader(request: ExportRequest, type: String): Map<String, Any?> = mapOf(
        "type" to type,
        "generatedAt" to baseMeta(request)["generatedAt"],
        "user" to mapOf(
            "name" to request.meta.userDisplayName,
            "email" to request.meta.userEmail,
        ),
        "locale" to request.meta.locale,
        "currency" to request.meta.currency,
    )
}
