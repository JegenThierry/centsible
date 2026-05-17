package beer.thierry.centsibleexport.postprocess

import beer.thierry.centsible.api.model.export.PostProcessingType
import org.springframework.stereotype.Component

@Component
class PostProcessorRegistry(processors: List<PostProcessor>) {
    private val byType: Map<PostProcessingType, PostProcessor> = processors.associateBy { it.supports() }

    fun forType(type: PostProcessingType): PostProcessor =
        byType[type] ?: throw IllegalStateException("No post-processor registered for type $type")
}
