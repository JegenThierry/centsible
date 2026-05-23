package beer.thierry.centsible.core.service

import beer.thierry.centsible.core.services.integrations.OAuthStateStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class OAuthStateStoreTest {

    @Test
    fun `mint returns a non-empty token`() {
        val store = OAuthStateStore()
        val token = store.mint(UUID.randomUUID(), UUID.randomUUID(), "paypal")

        assertNotNull(token)
        // 16 bytes of entropy → 22-char URL-safe base64 (no padding).
        assertEquals(22, token.length)
    }

    @Test
    fun `consume returns the minted record on first use`() {
        val store = OAuthStateStore()
        val userId = UUID.randomUUID()
        val connectionId = UUID.randomUUID()
        val token = store.mint(userId, connectionId, "paypal")

        val record = store.consume(token, "paypal")
        assertNotNull(record)
        assertEquals(userId, record!!.userId)
        assertEquals(connectionId, record.connectionId)
        assertEquals("paypal", record.providerKey)
    }

    @Test
    fun `consume returns null on replay`() {
        // Single-use semantics: the same token cannot be redeemed twice within the TTL.
        // This is the entire replay-protection property.
        val store = OAuthStateStore()
        val token = store.mint(UUID.randomUUID(), UUID.randomUUID(), "paypal")

        assertNotNull(store.consume(token, "paypal"))
        assertNull(store.consume(token, "paypal"))
    }

    @Test
    fun `consume returns null on unknown token`() {
        val store = OAuthStateStore()
        assertNull(store.consume("never-minted", "paypal"))
    }

    @Test
    fun `consume returns null on blank token`() {
        val store = OAuthStateStore()
        assertNull(store.consume("", "paypal"))
        assertNull(store.consume("   ", "paypal"))
    }

    @Test
    fun `consume rejects token minted for a different provider`() {
        // Defense-in-depth: a token minted for provider A must not complete provider B's
        // callback even if URL routing somehow misfires.
        val store = OAuthStateStore()
        val token = store.mint(UUID.randomUUID(), UUID.randomUUID(), "paypal")

        assertNull(store.consume(token, "banking-gocardless"))
        // The mismatched-provider attempt still consumes the token — by design, even a probe
        // burns it so an attacker can't iterate provider keys against a stolen state.
        assertNull(store.consume(token, "paypal"))
    }

    @Test
    fun `mint produces distinct tokens on consecutive calls`() {
        val store = OAuthStateStore()
        val t1 = store.mint(UUID.randomUUID(), UUID.randomUUID(), "p")
        val t2 = store.mint(UUID.randomUUID(), UUID.randomUUID(), "p")
        assertNotEquals(t1, t2)
    }
}
