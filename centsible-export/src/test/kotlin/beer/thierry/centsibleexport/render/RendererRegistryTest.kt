package beer.thierry.centsibleexport.render

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.export.proto.ExportRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

private class FakeRenderer(
    private val type: ExportType,
    private val format: ExportFormat,
) : ExportRenderer {
    override fun supports(): ExportType = type
    override fun supportedFormat(): ExportFormat = format
    override fun render(request: ExportRequest): RenderedExport =
        RenderedExport(pdf = ByteArray(0), filename = "fake-${type.name}.${format.name.lowercase()}")
}

class RendererRegistryTest {

    @Test
    fun `find returns the renderer registered for type and format`() {
        val txnPdf = FakeRenderer(ExportType.TRANSACTIONS, ExportFormat.PDF)
        val txnCsv = FakeRenderer(ExportType.TRANSACTIONS, ExportFormat.CSV)
        val txnJson = FakeRenderer(ExportType.TRANSACTIONS, ExportFormat.JSON)

        val registry = RendererRegistry(listOf(txnPdf, txnCsv, txnJson))

        assertSame(txnPdf, registry.find(ExportType.TRANSACTIONS, ExportFormat.PDF))
        assertSame(txnCsv, registry.find(ExportType.TRANSACTIONS, ExportFormat.CSV))
        assertSame(txnJson, registry.find(ExportType.TRANSACTIONS, ExportFormat.JSON))
    }

    @Test
    fun `find throws when no renderer is registered for the type and format combination`() {
        val onlyPdf = FakeRenderer(ExportType.TRANSACTIONS, ExportFormat.PDF)
        val registry = RendererRegistry(listOf(onlyPdf))

        val ex = assertThrows(IllegalStateException::class.java) {
            registry.find(ExportType.TRANSACTIONS, ExportFormat.CSV)
        }
        assertEquals(
            "No renderer registered for type=TRANSACTIONS format=CSV",
            ex.message,
        )
    }

    @Test
    fun `RendererKey equality is structural`() {
        assertEquals(
            RendererKey(ExportType.LENDINGS_ALL, ExportFormat.JSON),
            RendererKey(ExportType.LENDINGS_ALL, ExportFormat.JSON),
        )
    }
}
