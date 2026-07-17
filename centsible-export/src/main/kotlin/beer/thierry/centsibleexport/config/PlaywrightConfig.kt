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
