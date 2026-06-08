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
        val store = OAuthStateStore()
        val token = store.mint(UUID.randomUUID(), UUID.randomUUID(), "paypal")

        assertNull(store.consume(token, "banking-gocardless"))
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
