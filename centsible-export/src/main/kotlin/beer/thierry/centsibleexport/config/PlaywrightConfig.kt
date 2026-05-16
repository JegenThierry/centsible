package beer.thierry.centsibleexport.config

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlaywrightConfig {

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser

    @Bean
    fun playwright(): Playwright {
        playwright = Playwright.create()
        return playwright
    }

    @Bean
    fun browser(playwright: Playwright): Browser {
        browser = playwright.chromium().launch(
            BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(listOf("--no-sandbox", "--disable-dev-shm-usage"))
        )
        return browser
    }

    @PreDestroy
    fun shutdown() {
        runCatching { browser.close() }
        runCatching { playwright.close() }
    }
}
