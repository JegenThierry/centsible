package beer.thierry.centsible.core.services.email

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticMessageSource
import java.util.Locale

class EmailTemplateRendererTest {

    private val renderer = EmailTemplateRenderer(StaticMessageSource())

    private fun copy(heading: String = "Welcome") = EmailCopy(
        subject = "Subject line",
        preheader = "Preheader",
        eyebrow = "Eyebrow",
        heading = heading,
        body = "Body text",
        cta = "Confirm",
        fallbackPrompt = "Or paste this link:",
        expiryNotice = "Expires soon",
        footer = "Footer",
        htmlLang = "en",
    )

    @Test
    fun `renderHtml injects copy fields and the cta url`() {
        val html = renderer.renderHtml(copy(), "https://example.com/confirm?token=abc&x=1")

        assertTrue(html.contains("<title>Subject line</title>"))
        assertTrue(html.contains("Body text"))
        assertTrue(html.contains("Confirm"))
        assertTrue(html.contains("https://example.com/confirm?token=abc&amp;x=1"))
    }

    @Test
    fun `renderHtml emits the already-escaped heading raw, not double-escaped`() {
        val html = renderer.renderHtml(copy(heading = "Hi &lt;script&gt;"), "https://example.com")

        assertTrue(html.contains("Hi &lt;script&gt;"))
        assertFalse(html.contains("&amp;lt;script&amp;gt;"))
    }

    @Test
    fun `renderGreetingName escapes html in the supplied name`() {
        val out = renderer.renderGreetingName("<script>alert(1)</script>", "fallback.key", Locale.ENGLISH)
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", out)
    }
}
