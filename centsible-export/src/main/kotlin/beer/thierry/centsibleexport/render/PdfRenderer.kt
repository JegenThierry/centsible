package beer.thierry.centsibleexport.render

import beer.thierry.centsibleexport.config.BrowserPool
import beer.thierry.centsibleexport.worker.elapsedMsSince
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.Margin
import io.pebbletemplates.pebble.PebbleEngine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.StringWriter

@Component
class PdfRenderer(
    private val pebbleEngine: PebbleEngine,
    private val browserPool: BrowserPool,
    // Bounds setContent/waitForLoadState so a template that references an unreachable resource
    // (NETWORKIDLE would otherwise block forever) cannot pin the shared scheduler thread past the lease.
    @param:Value("\${export.render.timeout-ms:60000}") private val renderTimeoutMs: Double,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Renders the Pebble [template] with [context] to HTML, then prints it to PDF via headless Chromium. */
    fun renderHtmlToPdf(template: String, context: Map<String, Any?>): ByteArray {
        log.debug("Rendering PDF template={}", template)
        val startNanos = System.nanoTime()
        try {
            val bytes = htmlToPdfBytes(renderHtml(template, context))
            val elapsedMs = elapsedMsSince(startNanos)
            log.info("Rendered PDF template={} bytes={} elapsedMs={}", template, bytes.size, elapsedMs)
            return bytes
        } catch (ex: Exception) {
            log.error("Failed to render PDF template={}", template, ex)
            throw ex
        }
    }

    private fun renderHtml(template: String, context: Map<String, Any?>): String {
        val pebble = pebbleEngine.getTemplate(template)
        val writer = StringWriter()
        pebble.evaluate(writer, context)
        return writer.toString()
    }

    private fun htmlToPdfBytes(html: String): ByteArray = browserPool.withPage { page ->
        page.setDefaultTimeout(renderTimeoutMs)
        page.setContent(html)
        page.waitForLoadState(LoadState.NETWORKIDLE)
        page.pdf(
            Page.PdfOptions()
                .setFormat("A4")
                .setPrintBackground(true)
                .setDisplayHeaderFooter(true)
                .setHeaderTemplate(HEADER_TEMPLATE)
                .setFooterTemplate(FOOTER_TEMPLATE)
                .setMargin(
                    Margin().setTop("18mm").setBottom("22mm").setLeft("16mm").setRight("16mm")
                )
        )
    }

    companion object {
        private val HEADER_TEMPLATE = """
            <div style="font-size:7pt;color:#9ca3af;width:100%;padding:0 16mm;text-align:right;">
              <span class="title"></span>
            </div>
        """.trimIndent()

        private val FOOTER_TEMPLATE = """
            <div style="font-size:7pt;color:#6b7280;width:100%;padding:0 16mm;display:flex;justify-content:space-between;">
              <span>Centsible</span>
              <span>Page <span class="pageNumber"></span> of <span class="totalPages"></span></span>
            </div>
        """.trimIndent()
    }
}
