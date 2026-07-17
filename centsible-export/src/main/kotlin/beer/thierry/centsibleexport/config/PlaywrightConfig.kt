package beer.thierry.centsibleexport.config

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlaywrightConfig {

    private lateinit var playwright: Playwright
    private lateinit var browserPool: BrowserPool

    @Bean
    fun playwright(): Playwright = Playwright.create().also { playwright = it }

    @Bean
    fun browserPool(playwright: Playwright): BrowserPool = BrowserPool(playwright).also { browserPool = it }

    @PreDestroy
    fun shutdown() {
        runCatching { browserPool.close() }
        runCatching { playwright.close() }
    }
}

/**
 * Owns the headless Chromium process and hands out pages through [withPage].
 *
 * Chromium is launched eagerly so a broken image fails startup rather than the first export, but the
 * process can still die later under container memory pressure (the reason for --disable-dev-shm-usage).
 * A dead browser fails every subsequent newPage(), and the worker treats a render failure as terminal,
 * so without recovery the service would poll healthily while failing 100% of PDF jobs until restarted.
 * [withPage] therefore relaunches whenever the browser is no longer connected.
 *
 * Access is serialized: Playwright's Java client pumps the driver pipe on the *calling* thread and holds
 * no lock of its own, so concurrent calls would corrupt its callback map. Renders already run one at a
 * time (only the fixed-delay ExportJobWorker renders), so the lock costs nothing and makes the relaunch
 * safe against the scheduler pool handing successive ticks to different threads.
 */
class BrowserPool(private val playwright: Playwright) {

    private val log = LoggerFactory.getLogger(BrowserPool::class.java)
    private var browser: Browser = launch()

    @Synchronized
    fun <T> withPage(block: (Page) -> T): T = connectedBrowser().newPage().use(block)

    private fun connectedBrowser(): Browser {
        if (browser.isConnected) return browser
        log.warn("Chromium is no longer connected; relaunching before rendering")
        runCatching { browser.close() }
        return launch().also { browser = it }
    }

    private fun launch(): Browser = playwright.chromium().launch(
        BrowserType.LaunchOptions()
            .setHeadless(true)
            .setArgs(listOf("--no-sandbox", "--disable-dev-shm-usage"))
    )

    @Synchronized
    fun close() {
        runCatching { browser.close() }
    }
}
