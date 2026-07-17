package beer.thierry.centsible.integrations.paypal

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.model.integrations.Capability
import beer.thierry.centsible.api.model.integrations.FieldType
import beer.thierry.centsible.api.model.integrations.ProviderContext
import beer.thierry.centsible.integrations.support.IntegrationApiException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.web.client.ResourceAccessException
import java.math.BigDecimal
import java.time.Duration
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

    /**
     * fetchUserInfo goes through exchangeOrThrow, so a REST app without the 'openid profile email'
     * scopes surfaces as IntegrationApiException — not RestClientResponseException. Mocking the
     * latter would pass against a module that cannot catch the former at all.
     */
    @Test
    fun `listExternalAccounts falls back to PayPal label when userinfo returns a non-2xx status`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        `when`(client.fetchUserInfo(anyArg(), anyArg()))
            .thenThrow(
                IntegrationApiException(
                    provider = "PayPal",
                    status = HttpStatus.FORBIDDEN,
                    body = "missing scope",
                    context = "identity/userinfo",
                )
            )
        val module = PaypalProviderModule(client)

        val accounts = module.listExternalAccounts(ctx())

        assertEquals("PayPal", accounts.single().name)
    }

    @Test
    fun `listExternalAccounts falls back to PayPal label when userinfo cannot be reached`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        `when`(client.fetchUserInfo(anyArg(), anyArg()))
            .thenThrow(ResourceAccessException("connect timed out"))
        val module = PaypalProviderModule(client)

        val accounts = module.listExternalAccounts(ctx())

        assertEquals("PayPal", accounts.single().name)
    }

    @Test
    fun `listExternalAccounts propagates a token failure rather than masking it as a display name`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenThrow(
                IntegrationApiException(
                    provider = "PayPal",
                    status = HttpStatus.UNAUTHORIZED,
                    body = "invalid_client",
                    context = "oauth2/token",
                )
            )
        val module = PaypalProviderModule(client)

        assertThrows(IntegrationApiException::class.java) {
            module.listExternalAccounts(ctx())
        }
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

    /**
     * A long backfill must not be accumulated in one list, nor restarted from the configured start
     * on every failure: one 31-day window is fetched per call and the cursor advances to its end,
     * so the next poll resumes there.
     */
    @Test
    fun `importSince fetches a single window per call and advances the cursor to that window's end`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        `when`(client.fetchTransactions(anyArg(), anyArg(), anyArg(), anyArg(), anyIntArg())).thenReturn(
            TransactionsResponse(transactionDetails = emptyList(), page = 1, totalPages = 1)
        )
        val module = PaypalProviderModule(client)

        val cursor = Instant.now().minus(Duration.ofDays(200))
        val page = module.importSince(ctx = ctx(), cursor = cursor.toString())

        verify(client, times(1)).fetchTransactions(anyArg(), anyArg(), anyArg(), anyArg(), anyIntArg())
        val nextCursor = Instant.parse(page.nextCursor!!)
        assertEquals(cursor.plus(Duration.ofDays(31)), nextCursor)
        assertTrue(nextCursor.isBefore(Instant.now()), "backfill is not yet caught up")
    }

    @Test
    fun `importSince stops the window at now when the remaining backlog is under one window`() {
        val client = mock(PaypalHttpClient::class.java)
        `when`(client.obtainAccessToken(anyArg(), anyArg(), anyArg()))
            .thenReturn(TokenResponse(accessToken = "tok"))
        `when`(client.fetchTransactions(anyArg(), anyArg(), anyArg(), anyArg(), anyIntArg())).thenReturn(
            TransactionsResponse(transactionDetails = emptyList(), page = 1, totalPages = 1)
        )
        val module = PaypalProviderModule(client)

        val cursor = Instant.now().minus(Duration.ofDays(3))
        val page = module.importSince(ctx = ctx(), cursor = cursor.toString())

        verify(client, times(1)).fetchTransactions(anyArg(), anyArg(), anyArg(), anyArg(), anyIntArg())
        val nextCursor = Instant.parse(page.nextCursor!!)
        assertTrue(nextCursor.isAfter(cursor.plus(Duration.ofDays(2))), "cursor advanced towards now")
        assertTrue(nextCursor.isBefore(Instant.now()), "cursor never runs past now")
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
