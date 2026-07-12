package beer.thierry.centsible.integrations.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

class HttpTimeoutPropertiesTest {

    private fun bind(props: Map<String, Any>): HttpTimeoutProperties =
        Binder(MapConfigurationPropertySource(props))
            .bind("integrations.http", HttpTimeoutProperties::class.java)
            .orElseGet { HttpTimeoutProperties() }

    @Test
    fun `defaults match the values the provider configs previously hardcoded`() {
        val props = HttpTimeoutProperties()
        assertEquals(10_000L, props.connectTimeoutMs)
        assertEquals(30_000L, props.readTimeoutMs)
    }

    @Test
    fun `binds connect and read timeouts from the integrations http prefix`() {
        val props = bind(
            mapOf(
                "integrations.http.connect-timeout-ms" to "2500",
                "integrations.http.read-timeout-ms" to "9000",
            ),
        )
        assertEquals(2_500L, props.connectTimeoutMs)
        assertEquals(9_000L, props.readTimeoutMs)
    }

    @Test
    fun `unset properties fall back to defaults`() {
        val props = bind(mapOf("integrations.http.connect-timeout-ms" to "1234"))
        assertEquals(1_234L, props.connectTimeoutMs)
        assertEquals(30_000L, props.readTimeoutMs)
    }

    @Test
    fun `requestFactory builds a usable factory`() {
        assertNotNull(HttpTimeoutProperties().requestFactory())
    }
}
