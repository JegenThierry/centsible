package beer.thierry.centsiblerest.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecretRedactingConverterTest {

    @Test
    fun `redacts json-style access_token value`() {
        val msg = """request: {"access_token":"abc.def.ghi","other":"keep"}"""
        val out = SecretRedactingConverter.redact(msg)

        assertTrue(out.contains("\"access_token\":\"***REDACTED***\""))
        assertTrue(out.contains("\"other\":\"keep\""))
    }

    @Test
    fun `redacts every json-style secret-bearing key`() {
        val keys = listOf(
            "refresh_token", "client_secret", "secret_key",
            "secret_id", "api_key", "password",
        )
        for (key in keys) {
            val msg = """{"$key":"value-$key"}"""
            val out = SecretRedactingConverter.redact(msg)
            assertEquals("""{"$key":"***REDACTED***"}""", out)
        }
    }

    @Test
    fun `redacts case-insensitive Authorization header`() {
        val msg = "calling provider — Authorization: Bearer abc123def456"
        val out = SecretRedactingConverter.redact(msg)
        assertEquals("calling provider — Authorization: Bearer ***REDACTED***", out)
    }

    @Test
    fun `redacts Basic authorization`() {
        val msg = "Authorization: Basic dXNlcjpwYXNzd29yZA=="
        val out = SecretRedactingConverter.redact(msg)
        assertEquals("Authorization: Basic ***REDACTED***", out)
    }

    @Test
    fun `redacts authorization inside JSON envelope`() {
        val msg = """{"authorization":"Bearer eyJabc123"}"""
        val out = SecretRedactingConverter.redact(msg)
        assertEquals("""{"authorization":"Bearer ***REDACTED***"}""", out)
    }

    @Test
    fun `redacts query-string style access_token`() {
        val msg = "redirect: https://provider/callback?access_token=ABCDEF&state=keep"
        val out = SecretRedactingConverter.redact(msg)
        assertEquals(
            "redirect: https://provider/callback?access_token=***REDACTED***&state=keep",
            out,
        )
    }

    @Test
    fun `does not redact unrelated keys`() {
        val msg = """{"username":"alice","email":"alice@x"}"""
        assertEquals(msg, SecretRedactingConverter.redact(msg))
    }

    @Test
    fun `leaves messages without secrets untouched`() {
        val msg = "plain log line with no secrets"
        assertEquals(msg, SecretRedactingConverter.redact(msg))
    }
}
