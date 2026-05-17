package beer.thierry.centsibleexport.render

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.Margin
import io.pebbletemplates.pebble.PebbleEngine
import org.springframework.stereotype.Component
import java.io.StringWriter

@Component
class PdfRenderer(
    private val pebbleEngine: PebbleEngine,
    private val browser: Browser,
) {

    fun renderHtmlToPdf(template: String, context: Map<String, Any?>): ByteArray =
        htmlToPdfBytes(renderHtml(template, context))

    private fun renderHtml(template: String, context: Map<String, Any?>): String {
        val pebble = pebbleEngine.getTemplate(template)
        val writer = StringWriter()
        pebble.evaluate(writer, context)
        return writer.toString()
    }

    private fun htmlToPdfBytes(html: String): ByteArray = browser.newPage().use { page ->
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
