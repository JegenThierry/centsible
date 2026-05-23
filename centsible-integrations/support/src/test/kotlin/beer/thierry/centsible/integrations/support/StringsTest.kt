package beer.thierry.centsible.integrations.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StringsTest {

    @Test
    fun `firstNonBlank returns null when no candidates supplied`() {
        assertNull(firstNonBlank())
    }

    @Test
    fun `firstNonBlank returns null when every candidate is null or blank`() {
        assertNull(firstNonBlank(null, "", "   ", null))
    }

    @Test
    fun `firstNonBlank returns the first non-blank candidate`() {
        assertEquals("hit", firstNonBlank(null, " ", "hit", "later"))
    }

    @Test
    fun `firstNonBlank does not trim its result`() {
        assertEquals(" padded ", firstNonBlank(null, " padded ", "fallback"))
    }

    @Test
    fun `requireNonBlank accepts a non-blank value`() {
        requireNonBlank("ok", "field")
    }

    @Test
    fun `requireNonBlank throws on empty value`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            requireNonBlank("", "clientId")
        }
        assertEquals("clientId must not be blank", ex.message)
    }

    @Test
    fun `requireNonBlank throws on whitespace-only value`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            requireNonBlank("   ", "token")
        }
        assertEquals("token must not be blank", ex.message)
    }
}
