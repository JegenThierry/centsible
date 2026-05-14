package beer.thierry.budgetplannerexport.render

import beer.thierry.budgetplanner.api.model.export.ExportType
import org.springframework.stereotype.Component

@Component
class RendererRegistry(renderers: List<ExportRenderer>) {
    private val byType: Map<ExportType, ExportRenderer> = renderers.associateBy { it.supports() }

    fun forType(type: ExportType): ExportRenderer =
        byType[type] ?: throw IllegalStateException("No renderer registered for export type $type")
}
