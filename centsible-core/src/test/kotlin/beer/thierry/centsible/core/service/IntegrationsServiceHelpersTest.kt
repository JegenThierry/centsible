package beer.thierry.centsible.core.services.integrations

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import java.util.UUID

class IntegrationsServiceHelpersTest {

    @Test
    fun `syntheticUser carries only the id field`() {
        val id = UUID.randomUUID()
        val user = syntheticUser(id)
        assertEquals(id, user.id)
    }

    @Test
    fun `mergedWith returns a new map with patch entries layered on top`() {
        val original = mapOf("a" to "1", "b" to "2")
        val patch = mapOf("b" to "OVERWRITTEN", "c" to "3")

        val merged = original.mergedWith(patch)

        assertEquals("1", merged["a"])
        assertEquals("OVERWRITTEN", merged["b"])
        assertEquals("3", merged["c"])
    }

    @Test
    fun `mergedWith does not mutate the receiver`() {
        val original = mapOf("a" to "1")
        original.mergedWith(mapOf("a" to "2", "b" to "3"))
        assertEquals(mapOf("a" to "1"), original)
    }

    @Test
    fun `mergedWith with an empty patch returns a fresh copy with the same entries`() {
        val original = mapOf("k" to "v")
        val merged = original.mergedWith(emptyMap())
        assertEquals(original, merged)
        assertNotSame(original, merged)
    }
}
