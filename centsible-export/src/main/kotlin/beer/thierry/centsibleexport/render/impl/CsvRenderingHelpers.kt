package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.ExportRenderer
import beer.thierry.centsibleexport.render.RenderedExport
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.slf4j.LoggerFactory
import java.io.StringWriter

class CsvBuilder {
    private val writer = StringWriter()
    private val printer = CSVPrinter(writer, CSVFormat.RFC4180)
    private var rowCount: Int = 0

    fun row(vararg cells: Any?): CsvBuilder = apply {
        printer.printRecord(*cells.map { sanitizeCell(it) }.toTypedArray())
        rowCount += 1
    }

    fun row(cells: List<Any?>): CsvBuilder = apply {
        printer.printRecord(cells.map { sanitizeCell(it) })
        rowCount += 1
    }

    fun rowCount(): Int = rowCount

    private fun sanitizeCell(cell: Any?): String {
        val text = cell?.toString() ?: ""
        return if (cell is String && text.isNotEmpty() && text[0] in FORMULA_TRIGGERS) "'$text" else text
    }

    fun bytes(): ByteArray {
        printer.flush()
        return UTF8_BOM + writer.toString().toByteArray(Charsets.UTF_8)
    }

    private companion object {
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        val FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t', '\r')
    }
}

abstract class CsvExportRenderer : ExportRenderer {
    private val log = LoggerFactory.getLogger(javaClass)

    final override fun supportedFormat(): ExportFormat = ExportFormat.CSV

    final override fun render(request: ExportRequest): RenderedExport {
        log.debug("Rendering CSV renderer={} type={}", javaClass.simpleName, supports())
        try {
            val csv = buildCsv(request)
            val bytes = csv.bytes()
            log.info(
                "Rendered CSV renderer={} type={} rows={} bytes={}",
                javaClass.simpleName, supports(), csv.rowCount(), bytes.size,
            )
            return RenderedExport(
                pdf = bytes,
                filename = "${filenameStem(request)}-${filenameTimestamp()}.csv",
            )
        } catch (ex: Exception) {
            log.error("Failed to render CSV renderer={} type={}", javaClass.simpleName, supports(), ex)
            throw ex
        }
    }

    protected abstract fun buildCsv(request: ExportRequest): CsvBuilder
    protected abstract fun filenameStem(request: ExportRequest): String
}
