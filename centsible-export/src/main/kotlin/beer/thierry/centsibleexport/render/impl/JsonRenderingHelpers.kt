package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.ExportRenderer
import beer.thierry.centsibleexport.render.RenderedExport
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory

abstract class JsonExportRenderer(
    private val objectMapper: ObjectMapper,
) : ExportRenderer {
    private val log = LoggerFactory.getLogger(javaClass)

    final override fun supportedFormat(): ExportFormat = ExportFormat.JSON

    final override fun render(request: ExportRequest): RenderedExport {
        log.debug("Rendering JSON renderer={} type={}", javaClass.simpleName, supports())
        try {
            val envelope = buildJson(request)
            val bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(envelope)
            log.info(
                "Rendered JSON renderer={} type={} bytes={}",
                javaClass.simpleName, supports(), bytes.size,
            )
            return RenderedExport(
                pdf = bytes,
                filename = "${filenameStem(request)}-${filenameTimestamp()}.json",
            )
        } catch (ex: Exception) {
            log.error("Failed to render JSON renderer={} type={}", javaClass.simpleName, supports(), ex)
            throw ex
        }
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
