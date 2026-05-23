package beer.thierry.centsibleexport.postprocess

import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.PostProcessingType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

private class FakePostProcessor(private val type: PostProcessingType) : PostProcessor {
    override fun supports(): PostProcessingType = type
    override fun execute(job: ExportJobDTO, pdf: ByteArray, pdfFilename: String, config: Map<String, Any?>) = Unit
}

class PostProcessorRegistryTest {

    @Test
    fun `forType returns the processor registered for the type`() {
        val email = FakePostProcessor(PostProcessingType.SEND_EMAIL)
        val registry = PostProcessorRegistry(listOf(email))

        assertSame(email, registry.forType(PostProcessingType.SEND_EMAIL))
    }

    @Test
    fun `forType throws with a descriptive message when no processor is registered`() {
        val registry = PostProcessorRegistry(emptyList())

        val ex = assertThrows(IllegalStateException::class.java) {
            registry.forType(PostProcessingType.SEND_EMAIL)
        }
        assertEquals("No post-processor registered for type SEND_EMAIL", ex.message)
    }
}
