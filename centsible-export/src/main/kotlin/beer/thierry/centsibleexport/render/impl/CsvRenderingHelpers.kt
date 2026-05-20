package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.ExportRenderer
import beer.thierry.centsibleexport.render.RenderedExport

/**
 * Lightweight RFC 4180-style CSV writer. Quotes cells that contain a comma, quote, or
 * newline; doubles embedded quotes. Lines end with CRLF. Output is UTF-8 with a BOM so
 * Excel opens it correctly on Windows.
 */
class CsvBuilder {
    private val sb = StringBuilder()

    fun row(vararg cells: Any?): CsvBuilder = apply {
        sb.append(cells.joinToString(",") { quote(it?.toString() ?: "") })
        sb.append("\r\n")
    }

    fun row(cells: List<Any?>): CsvBuilder = apply {
        sb.append(cells.joinToString(",") { quote(it?.toString() ?: "") })
        sb.append("\r\n")
    }

    fun bytes(): ByteArray = UTF8_BOM + sb.toString().toByteArray(Charsets.UTF_8)

    private fun quote(s: String): String {
        val needsQuoting = s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuoting) "\"${s.replace("\"", "\"\"")}\"" else s
    }

    private companion object {
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}

/**
 * Base class that wires a CSV renderer into the [ExportRenderer] SPI. Subclasses only fill in
 * the per-export-type data fetch and column shape; framing (format, filename, byte packaging)
 * is handled here.
 */
abstract class CsvExportRenderer : ExportRenderer {
    final override fun supportedFormat(): ExportFormat = ExportFormat.CSV

    final override fun render(request: ExportRequest): RenderedExport {
        val csv = buildCsv(request)
        return RenderedExport(
            pdf = csv.bytes(), // Field name is historical; carries CSV bytes for CSV renderers.
            filename = "${filenameStem(request)}-${filenameTimestamp()}.csv",
        )
    }

    protected abstract fun buildCsv(request: ExportRequest): CsvBuilder
    protected abstract fun filenameStem(request: ExportRequest): String
}
