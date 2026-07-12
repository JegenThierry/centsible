package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.export.proto.ExportMeta
import beer.thierry.centsible.export.proto.ExportRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class RenderingHelpersTest {

    private fun request(
        locale: String = "en-GB",
        currency: String = "EUR",
        userDisplayName: String = "Alice",
        userEmail: String = "alice@example.com",
    ): ExportRequest = ExportRequest.newBuilder()
        .setMeta(
            ExportMeta.newBuilder()
                .setLocale(locale)
                .setCurrency(currency)
                .setUserDisplayName(userDisplayName)
                .setUserEmail(userEmail)
                .build()
        )
        .build()

    @Test
    fun `locale parses the meta tag when present`() {
        assertEquals(Locale.forLanguageTag("fr-FR"), request(locale = "fr-FR").locale())
    }

    @Test
    fun `locale falls back to en-GB when meta is blank`() {
        assertEquals(Locale.forLanguageTag("en-GB"), request(locale = "").locale())
    }

    @Test
    fun `baseMeta uses meta fields when present`() {
        val meta = baseMeta(request(userDisplayName = "Alice", userEmail = "a@x", currency = "USD"))
        assertEquals("Alice", meta["userName"])
        assertEquals("a@x", meta["userEmail"])
        assertEquals("USD", meta["currency"])
        assertEquals("en-GB", meta["locale"])
        assertTrue(meta["generatedAt"] is String)
    }

    @Test
    fun `baseMeta falls back to Account holder and EUR when fields are blank`() {
        val meta = baseMeta(request(userDisplayName = "", currency = "", locale = ""))
        assertEquals("Account holder", meta["userName"])
        assertEquals("EUR", meta["currency"])
        assertEquals("en-GB", meta["locale"])
    }

    @Test
    fun `slug lowercases and collapses non-alphanumeric runs to single dashes`() {
        assertEquals("hello-world", slug("Hello, World!"))
        assertEquals("a-b-c", slug("__a   b___c__"))
    }

    @Test
    fun `slug returns export for an all-symbol input`() {
        assertEquals("export", slug("!!!"))
        assertEquals("export", slug(""))
    }

    @Test
    fun `formatDate returns em-dash for null`() {
        assertEquals("—", formatDate(null, Locale.ENGLISH))
    }

    @Test
    fun `formatDate uses the supplied pattern and locale`() {
        val date = java.time.LocalDate.of(2024, 3, 14)
        assertEquals("14 Mar 2024", formatDate(date, Locale.ENGLISH))
        assertEquals("2024-03-14", formatDate(date, Locale.ENGLISH, pattern = "yyyy-MM-dd"))
    }
}
