package beer.thierry.centsibleexport.render

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.api.model.export.ExportType
import org.springframework.stereotype.Component

data class RendererKey(val type: ExportType, val format: ExportFormat)

@Component
class RendererRegistry(renderers: List<ExportRenderer>) {
    private val byKey: Map<RendererKey, ExportRenderer> = renderers.associateBy {
        RendererKey(it.supports(), it.supportedFormat())
    }

    fun find(type: ExportType, format: ExportFormat): ExportRenderer =
        byKey[RendererKey(type, format)]
            ?: throw IllegalStateException("No renderer registered for type=$type format=$format")
}
