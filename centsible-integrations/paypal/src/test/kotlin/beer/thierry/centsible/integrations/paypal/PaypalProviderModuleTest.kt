package beer.thierry.centsible.integrations.paypal

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.model.integrations.Capability
import beer.thierry.centsible.api.model.integrations.FieldType
import beer.thierry.centsible.api.model.integrations.ProviderContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class PaypalProviderModuleTest {

    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()
    private fun <T> anyIntArg(): T = org.mockito.ArgumentMatchers.anyInt() as T

    private fun ctx(
        config: Map<String, Any?> = mapOf(
            "clientId" to "client",
            "environment" to "sandbox",
        ),
        credentials: Map<String, String> = mapOf("clientSecret" to "shh"),
        lastCursor: String? = null,
    ) = ProviderContext(
        userId = UUID.randomUUID(),
        connectionId = UUID.randomUUID(),
        displayName = "PayPal",
        config = config,
        credentials = credentials,
        lastCursor = lastCursor,
    )

    @Test
    fun `descriptor advertises ACCOUNTS and TRANSACTIONS over API_KEY auth`() {
        val module = PaypalProviderModule(client = mock(PaypalHttpClient::class.java))

        assertEquals("paypal", module.descriptor.key)
        assertEquals(AuthType.API_KEY, module.descriptor.authType)
        assertEquals(setOf(Capability.ACCOUNTS, Capability.TRANSACTIONS), module.descriptor.capabilities)
    }

    @Test
    fun `clientSecret is the only secret field`() {
        // secret=true marks fields routed into the encrypted credentials store. PayPal's only
        // true secret is clientSecret — anything else slipping in here would silently bypass
        // the cipher.
        val module = PaypalProviderModule(client = mock(PaypalHttpClient::class.java))

        val secrets = module.descriptor.configFields.filter { it.secret }.map { it.name }
        assertEquals(listOf("clientSecret"), secrets)
    }

    @Test
    fun `environment field is a SELECT with live and sandbox options`() {
        val module = PaypalProviderModule(client = mock(PaypalHttpClient::class.java))

        val env = module.descriptor.configFields.single { it.name == "environment" }
        assertEquals(FieldType.SELECT, env.type)
        val values = env.options.map { it.value }
        assertTrue("live" in values)
        assertTrue("sandbox" in values)
    }

    @Test
    fun `testConnection delegates to obtainAccessToken with credentials from the cipher`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        val module = PaypalProviderModule(client)

        module.testConnection(ctx())

        verify(client).obtainAccessToken("sandbox", "client", "shh")
    }

    @Test
    fun `testConnection throws when clientSecret is missing from credentials`() {
        // Falling back to ctx.config for a secret field would defeat the cipher — the module
        // must refuse to start.
        val client = mock(PaypalHttpClient::class.java)
        val module = PaypalProviderModule(client)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            module.testConnection(ctx(credentials = emptyMap()))
        }
        assertTrue(ex.message!!.contains("clientSecret"))
        verify(client, never()).obtainAccessToken(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `testConnection throws when clientId is missing from config`() {
        val client = mock(PaypalHttpClient::class.java)
        val module = PaypalProviderModule(client)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            module.testConnection(ctx(config = mapOf("environment" to "live")))
        }
        assertTrue(ex.message!!.contains("clientId"))
    }

    @Test
    fun `testConnection defaults missing environment to live`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        val module = PaypalProviderModule(client)

        module.testConnection(ctx(config = mapOf("clientId" to "client")))

        verify(client).obtainAccessToken("live", "client", "shh")
    }

    @Test
    fun `listExternalAccounts uses email enrichment when available`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        `when`(client.fetchUserInfo(anyArg(), anyArg()))
            .thenReturn(UserInfoResponse(userId = "u", name = "Pay Pal", email = "user@example.com"))
        val module = PaypalProviderModule(client)

        val accounts = module.listExternalAccounts(ctx())

        assertEquals(1, accounts.size)
        assertEquals("user@example.com", accounts.single().name)
    }

    @Test
    fun `listExternalAccounts falls back to PayPal label when userinfo HTTP call fails`() {
        // The production code has two catch arms: RestClientResponseException (4xx/5xx) and
        // IOException (network failure). Stubbing IOException via Mockito requires a `throws`
        // declaration the Kotlin method does not have; the RestClientResponseException arm is
        // equally representative of "userinfo failed → fall back to default name".
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        `when`(client.fetchUserInfo(anyArg(), anyArg()))
            .thenThrow(
                org.springframework.web.client.RestClientResponseException(
                    "missing scope",
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Forbidden",
                    null, null, null,
                )
            )
        val module = PaypalProviderModule(client)

        val accounts = module.listExternalAccounts(ctx())

        assertEquals("PayPal", accounts.single().name)
    }

    @Test
    fun `listExternalAccounts uses the stable default external id`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        `when`(client.fetchUserInfo(anyArg(), anyArg()))
            .thenReturn(UserInfoResponse(userId = "u"))
        val module = PaypalProviderModule(client)

        val account = module.listExternalAccounts(ctx()).single()
        assertEquals("paypal:default", account.externalId)
        assertEquals("paypal", account.type)
    }

    @Test
    fun `importSince returns empty page with nextCursor when configured start is in the future`() {
        // start > end (now - safety margin) — the loop short-circuits without calling fetchTransactions.
        val futureStart = Instant.now().plusSeconds(86_400).toString().substring(0, 10)
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        val module = PaypalProviderModule(client)

        val page = module.importSince(
            ctx = ctx(
                config = mapOf(
                    "clientId" to "client",
                    "environment" to "sandbox",
                    "startDate" to futureStart,
                )
            ),
            cursor = null,
        )

        assertTrue(page.transactions.isEmpty())
        assertNotNull(page.nextCursor)
        verify(client, never()).fetchTransactions(anyArg(), anyArg(), anyArg(), anyArg(), anyIntArg())
    }

    @Test
    fun `importSince aggregates transactions across pages and skips unparseable rows`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        // The orchestrator pages through one chunk: page 1 returns totalPages=2 so the loop calls
        // page 2 next. Using thenReturn(first, second) keeps the test agnostic of how Mockito
        // resolves int-eq matchers — we just verify the in-order responses are honored.
        `when`(client.fetchTransactions(anyArg(), anyArg(), anyArg(), anyArg(), anyIntArg())).thenReturn(
            TransactionsResponse(
                transactionDetails = listOf(
                    TransactionDetail(
                        transactionInfo = TransactionInfo(
                            transactionId = "T1",
                            transactionInitiationDate = "2024-01-15T12:34:56Z",
                            transactionAmount = MoneyAmount(value = "12.34", currencyCode = "EUR"),
                            transactionSubject = "Coffee",
                        ),
                    ),
                    TransactionDetail(
                        transactionInfo = TransactionInfo(
                            transactionId = "T-bad-amount",
                            transactionInitiationDate = "2024-01-15T12:35:00Z",
                            transactionAmount = MoneyAmount(value = "not-a-number", currencyCode = "EUR"),
                        ),
                    ),
                ),
                page = 1,
                totalPages = 2,
            ),
            TransactionsResponse(
                transactionDetails = listOf(
                    TransactionDetail(
                        transactionInfo = TransactionInfo(
                            transactionId = "T2",
                            transactionInitiationDate = "2024-01-16T08:00:00Z",
                            transactionAmount = MoneyAmount(value = "-5.00", currencyCode = "EUR"),
                            transactionNote = "Refund",
                        ),
                    ),
                ),
                page = 2,
                totalPages = 2,
            ),
        )

        val module = PaypalProviderModule(client)

        val cursor = Instant.now().minusSeconds(10 * 86_400).toString()
        val page = module.importSince(ctx = ctx(), cursor = cursor)

        assertEquals(2, page.transactions.size)
        assertEquals(BigDecimal("12.34"), page.transactions[0].amount)
        assertEquals("Coffee", page.transactions[0].description)
        assertEquals(BigDecimal("-5.00"), page.transactions[1].amount)
        assertEquals("Refund", page.transactions[1].description)
    }

    @Test
    fun `importSince description falls back to PayPal event code when no labels present`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        `when`(client.fetchTransactions(anyArg(), anyArg(), anyArg(), anyArg(), anyIntArg())).thenReturn(
            TransactionsResponse(
                transactionDetails = listOf(
                    TransactionDetail(
                        transactionInfo = TransactionInfo(
                            transactionId = "T",
                            transactionEventCode = "T0006",
                            transactionInitiationDate = "2024-01-15T12:00:00Z",
                            transactionAmount = MoneyAmount(value = "1.00", currencyCode = "EUR"),
                        ),
                    ),
                ),
                page = 1,
                totalPages = 1,
            )
        )

        val module = PaypalProviderModule(client)
        val cursor = Instant.now().minusSeconds(2 * 86_400).toString()
        val page = module.importSince(ctx = ctx(), cursor = cursor)

        assertEquals("PayPal T0006", page.transactions.single().description)
    }
}
