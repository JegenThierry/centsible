package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.ExportRenderer
import beer.thierry.centsibleexport.render.RenderedExport
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.StringWriter

/**
 * Thin facade over Apache Commons CSV's [CSVPrinter] using [CSVFormat.RFC4180] (CRLF + double-
 * quoted escaping). Output is UTF-8 with a BOM so Excel on Windows treats the file as UTF-8 by
 * default; without the BOM Excel falls back to its system codepage and mangles non-ASCII.
 */
class CsvBuilder {
    private val writer = StringWriter()
    private val printer = CSVPrinter(writer, CSVFormat.RFC4180)

    fun row(vararg cells: Any?): CsvBuilder = apply {
        printer.printRecord(*cells.map { it?.toString() ?: "" }.toTypedArray())
    }

    fun row(cells: List<Any?>): CsvBuilder = apply {
        printer.printRecord(cells.map { it?.toString() ?: "" })
    }

    fun bytes(): ByteArray {
        printer.flush()
        return UTF8_BOM + writer.toString().toByteArray(Charsets.UTF_8)
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
