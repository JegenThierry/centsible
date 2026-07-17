package beer.thierry.centsible.integrations.banking.gocardless

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.model.integrations.Capability
import beer.thierry.centsible.api.model.integrations.FieldType
import beer.thierry.centsible.api.model.integrations.OAuthCallbackRequest
import beer.thierry.centsible.api.model.integrations.OAuthCredentialEnvelope
import beer.thierry.centsible.api.model.integrations.OAuthStartRequest
import beer.thierry.centsible.api.model.integrations.ProviderContext
import beer.thierry.centsible.api.model.integrations.RemoteOptionsRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GoCardlessProviderModuleTest {

    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()
    private fun anyIntArg(): Int = org.mockito.ArgumentMatchers.anyInt()
    private fun <T> eqArg(value: T): T = org.mockito.ArgumentMatchers.eq(value) ?: value

    private fun ctx(
        config: Map<String, Any?> = mapOf(
            "country" to "DE",
            "institutionId" to "DEUTSCHEBANK_DEUTDEFF",
        ),
        credentials: Map<String, String> = emptyMap(),
        lastCursor: String? = null,
    ) = ProviderContext(
        userId = UUID.randomUUID(),
        connectionId = UUID.randomUUID(),
        displayName = "Deutsche Bank",
        config = config,
        credentials = credentials,
        lastCursor = lastCursor,
    )

    private fun newModule(client: GoCardlessHttpClient = mock(GoCardlessHttpClient::class.java)) =
        GoCardlessProviderModule(client, minSyncIntervalSeconds = 3600L)

    @Test
    fun `descriptor advertises OAuth2 with accounts transactions and oauth flow capabilities`() {
        val module = newModule()

        assertEquals("banking-gocardless", module.descriptor.key)
        assertEquals(AuthType.OAUTH2, module.descriptor.authType)
        assertEquals(
            setOf(Capability.ACCOUNTS, Capability.TRANSACTIONS, Capability.OAUTH_FLOW),
            module.descriptor.capabilities,
        )
    }

    @Test
    fun `descriptor declares institutionId as a SELECT_REMOTE that depends on country`() {
        val module = newModule()

        val inst = module.descriptor.configFields.single { it.name == "institutionId" }
        assertEquals(FieldType.SELECT_REMOTE, inst.type)
        assertEquals(listOf("country"), inst.dependsOn)
    }

    @Test
    fun `descriptor exposes an OAUTH_LAUNCH affordance for the bank consent button`() {
        val module = newModule()

        val launch = module.descriptor.configFields.single { it.name == "oauthLaunch" }
        assertEquals(FieldType.OAUTH_LAUNCH, launch.type)
    }

    @Test
    fun `testConnection accepts a valid country and institution`() {
        newModule().testConnection(ctx())
    }

    @Test
    fun `testConnection rejects an unsupported country code`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            newModule().testConnection(ctx(config = mapOf("country" to "ZZ", "institutionId" to "FOO_BANK")))
        }
        assertTrue(ex.message!!.contains("ZZ"))
    }

    @Test
    fun `testConnection rejects when country is missing`() {
        assertThrows(IllegalArgumentException::class.java) {
            newModule().testConnection(ctx(config = mapOf("institutionId" to "FOO_BANK")))
        }
    }

    @Test
    fun `testConnection rejects an obviously-bad institution id`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            newModule().testConnection(ctx(config = mapOf("country" to "DE", "institutionId" to "x")))
        }
        assertTrue(ex.message!!.contains("institutionId"))
    }

    @Test
    fun `fetchOptions returns empty list for unknown field`() {
        val client = mock(GoCardlessHttpClient::class.java)
        val module = newModule(client)

        val out = module.fetchOptions(
            ctx = ctx(),
            request = RemoteOptionsRequest(fieldName = "country"),
        )
        assertTrue(out.isEmpty())
    }

    @Test
    fun `fetchOptions returns empty list when country value is missing`() {
        val client = mock(GoCardlessHttpClient::class.java)
        val module = newModule(client)

        val out = module.fetchOptions(
            ctx = ctx(),
            request = RemoteOptionsRequest(fieldName = "institutionId", values = emptyMap()),
        )
        assertTrue(out.isEmpty())
    }

    @Test
    fun `fetchOptions filters institutions by query against name and bic`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.listInstitutions(anyArg())).thenReturn(
            listOf(
                Institution(id = "DB", name = "Deutsche Bank", bic = "DEUTDEFF"),
                Institution(id = "COM", name = "Commerzbank", bic = "COBADEFF"),
                Institution(id = "ING", name = "ING", bic = "INGDDEFF"),
            )
        )
        val module = newModule(client)

        val byName = module.fetchOptions(
            ctx = ctx(),
            request = RemoteOptionsRequest(
                fieldName = "institutionId",
                query = "deut",
                values = mapOf("country" to "DE"),
            ),
        )
        assertEquals(1, byName.size)
        assertEquals("DB", byName.single().value)

        val byBic = module.fetchOptions(
            ctx = ctx(),
            request = RemoteOptionsRequest(
                fieldName = "institutionId",
                query = "ingd",
                values = mapOf("country" to "DE"),
            ),
        )
        assertEquals("ING", byBic.single().value)
    }

    @Test
    fun `fetchOptions swallows upstream failures and returns empty list`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.listInstitutions(anyArg())).thenThrow(RuntimeException("upstream"))
        val module = newModule(client)

        val out = module.fetchOptions(
            ctx = ctx(),
            request = RemoteOptionsRequest(
                fieldName = "institutionId",
                values = mapOf("country" to "DE"),
            ),
        )
        assertTrue(out.isEmpty())
    }

    @Test
    fun `buildAuthorizationUrl stashes pending requisition and agreement ids in the config patch`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.createEndUserAgreement(anyArg(), anyIntArg())).thenReturn("AG-1")
        `when`(client.createRequisition(anyArg(), anyArg(), anyArg(), anyArg()))
            .thenReturn(RequisitionResponse(id = "REQ-1", link = "https://bank/consent", status = "CR"))

        val module = newModule(client)

        val start = module.buildAuthorizationUrl(
            OAuthStartRequest(
                connectionId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                displayName = "Deutsche Bank",
                config = mapOf("institutionId" to "DB", "historicalDays" to 30),
                redirectUri = "https://app/callback",
                state = "opaque-state",
            )
        )

        assertEquals("https://bank/consent", start.authorizationUrl)
        assertEquals("REQ-1", start.configPatch["pendingRequisitionId"])
        assertEquals("AG-1", start.configPatch["pendingAgreementId"])
    }

    @Test
    fun `buildAuthorizationUrl rejects empty institutionId`() {
        val module = newModule()

        assertThrows(IllegalArgumentException::class.java) {
            module.buildAuthorizationUrl(
                OAuthStartRequest(
                    connectionId = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    displayName = "x",
                    config = emptyMap(),
                    redirectUri = "https://app/callback",
                    state = "opaque",
                )
            )
        }
    }

    @Test
    fun `completeAuthorization returns delegated envelope and clears pending markers when consent OK`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchRequisition(eqArg("REQ-1"))).thenReturn(
            RequisitionResponse(id = "REQ-1", link = "https://bank", status = "LN", accounts = listOf("A1", "A2"))
        )
        val module = newModule(client)

        val result = module.completeAuthorization(
            OAuthCallbackRequest(
                connectionId = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                config = mapOf("pendingRequisitionId" to "REQ-1"),
                currentCredentials = emptyMap(),
                redirectUri = "https://app/callback",
            )
        )

        assertEquals("delegated", result.envelope.accessToken)
        assertNull(result.configPatch["pendingRequisitionId"])
        assertNull(result.configPatch["pendingAgreementId"])
        assertEquals("REQ-1", result.configPatch["requisitionId"])
        assertEquals(listOf("A1", "A2"), result.configPatch["accountIds"])
        assertNotNull(result.configPatch["consentExpiresAt"])
    }

    @Test
    fun `completeAuthorization rejects when consent status is not LN or GC`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchRequisition(anyArg())).thenReturn(
            RequisitionResponse(id = "REQ", link = "", status = "RJ")
        )
        val module = newModule(client)

        val ex = assertThrows(IllegalStateException::class.java) {
            module.completeAuthorization(
                OAuthCallbackRequest(
                    connectionId = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    config = mapOf("pendingRequisitionId" to "REQ"),
                    currentCredentials = emptyMap(),
                    redirectUri = "https://app/callback",
                )
            )
        }
        assertTrue(ex.message!!.contains("RJ"))
    }

    @Test
    fun `completeAuthorization rejects when no pendingRequisitionId in config`() {
        val module = newModule()

        assertThrows(IllegalArgumentException::class.java) {
            module.completeAuthorization(
                OAuthCallbackRequest(
                    connectionId = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    config = emptyMap(),
                    currentCredentials = emptyMap(),
                    redirectUri = "https://app/callback",
                )
            )
        }
    }

    @Test
    fun `refreshAccessToken returns existing envelope when PSD2 consent is still valid`() {
        val module = newModule()
        val envelope = OAuthCredentialEnvelope(accessToken = "delegated")
        val futureExpiry = Instant.now().plusSeconds(86_400).toString()

        val refreshed = module.refreshAccessToken(
            ctx(
                config = mapOf("consentExpiresAt" to futureExpiry),
                credentials = envelope.toMap(),
            )
        )

        assertEquals("delegated", refreshed.accessToken)
    }

    @Test
    fun `refreshAccessToken throws when PSD2 consent has expired`() {
        val module = newModule()
        val envelope = OAuthCredentialEnvelope(accessToken = "delegated")
        val pastExpiry = Instant.now().minusSeconds(60).toString()

        val ex = assertThrows(IllegalStateException::class.java) {
            module.refreshAccessToken(
                ctx(
                    config = mapOf("consentExpiresAt" to pastExpiry),
                    credentials = envelope.toMap(),
                )
            )
        }
        assertTrue(ex.message!!.contains("expired", ignoreCase = true))
    }

    @Test
    fun `refreshAccessToken throws when there's no envelope to refresh`() {
        val module = newModule()

        assertThrows(IllegalStateException::class.java) {
            module.refreshAccessToken(ctx())
        }
    }

    @Test
    fun `listExternalAccounts returns empty when no accountIds are stored`() {
        val client = mock(GoCardlessHttpClient::class.java)
        val module = newModule(client)

        val accounts = module.listExternalAccounts(ctx(config = mapOf("country" to "DE")))
        assertTrue(accounts.isEmpty())
    }

    @Test
    fun `listExternalAccounts falls back to id when account details fetch fails`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchAccountDetails(anyArg())).thenThrow(RuntimeException("upstream"))
        val module = newModule(client)

        val out = module.listExternalAccounts(
            ctx(config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1")))
        )
        assertEquals(1, out.size)
        assertEquals("ACC-1", out.single().externalId)
        assertEquals("ACC-1", out.single().name)
    }

    @Test
    fun `listExternalAccounts uses display name and currency from account details`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchAccountDetails(eqArg("ACC-1"))).thenReturn(
            AccountDetails(iban = "DE00", currency = "EUR", displayName = "Checking")
        )
        val module = newModule(client)

        val out = module.listExternalAccounts(
            ctx(config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1")))
        )
        assertEquals("Checking", out.single().name)
        assertEquals("EUR", out.single().currency)
    }

    @Test
    fun `importSince returns empty page when no accountIds`() {
        val client = mock(GoCardlessHttpClient::class.java)
        val module = newModule(client)

        val page = module.importSince(ctx(config = mapOf("country" to "DE")), cursor = null)
        assertTrue(page.transactions.isEmpty())
    }

    @Test
    fun `importSince maps booked transactions and skips unparseable amounts`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchAccountTransactions(eqArg("ACC-1"), anyArg())).thenReturn(
            TransactionsResponse(
                booked = listOf(
                    GoCardlessTransaction(
                        transactionId = "T1",
                        bookingDate = "2024-01-15",
                        transactionAmount = GoCardlessAmount(amount = "12.34", currency = "EUR"),
                        remittanceInformationUnstructured = "Coffee",
                        creditorName = "Cafe",
                    ),
                    GoCardlessTransaction(
                        transactionId = "T-bad",
                        bookingDate = "2024-01-16",
                        transactionAmount = GoCardlessAmount(amount = "NaN", currency = "EUR"),
                    ),
                    GoCardlessTransaction(
                        transactionId = "T2",
                        bookingDateTime = "2024-01-17T10:00:00Z",
                        transactionAmount = GoCardlessAmount(amount = "-5.00", currency = "EUR"),
                        remittanceInformationStructured = "Refund",
                    ),
                ),
            )
        )
        val module = newModule(client)

        val page = module.importSince(
            ctx = ctx(config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1"))),
            cursor = "2024-01-01",
        )

        assertEquals(2, page.transactions.size)
        assertEquals(BigDecimal("12.34"), page.transactions[0].amount)
        assertEquals("Coffee", page.transactions[0].description)
        assertEquals("Cafe", page.transactions[0].counterparty)
        assertEquals(BigDecimal("-5.00"), page.transactions[1].amount)
        assertEquals("Refund", page.transactions[1].description)
    }

    /**
     * A failed account means its window was never fetched. Returning normally would let the
     * orchestrator markSyncSuccess and advance the cursor past that window, losing every
     * transaction in it for good; throwing keeps the cursor and surfaces the failure.
     */
    @Test
    fun `importSince throws instead of advancing the cursor when an account fetch fails`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchAccountTransactions(eqArg("ACC-1"), anyArg()))
            .thenThrow(RuntimeException("upstream"))
        `when`(client.fetchAccountTransactions(eqArg("ACC-2"), anyArg())).thenReturn(
            TransactionsResponse(
                booked = listOf(
                    GoCardlessTransaction(
                        transactionId = "OK",
                        bookingDate = "2024-01-15",
                        transactionAmount = GoCardlessAmount(amount = "1.00", currency = "EUR"),
                    ),
                )
            )
        )
        val module = newModule(client)

        val ex = assertThrows(IllegalStateException::class.java) {
            module.importSince(
                ctx = ctx(
                    config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1", "ACC-2"))
                ),
                cursor = "2024-01-01",
            )
        }

        assertTrue(ex.message!!.contains("1 of 2"))
        assertTrue(ex.message!!.contains("2024-01-01"))
        verify(client).fetchAccountTransactions(eqArg("ACC-2"), anyArg())
    }

    @Test
    fun `importSince advances the cursor to today when every account was fetched`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchAccountTransactions(anyArg(), anyArg())).thenReturn(TransactionsResponse())
        val module = newModule(client)

        val page = module.importSince(
            ctx = ctx(config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1"))),
            cursor = "2024-01-01",
        )

        assertEquals(LocalDate.now().toString(), page.nextCursor)
    }

    private fun bookedResponse(vararg booked: GoCardlessTransaction) = TransactionsResponse(booked = booked.toList())

    private fun coffee(remittance: String? = "Coffee") = GoCardlessTransaction(
        bookingDate = "2024-01-15",
        transactionAmount = GoCardlessAmount(amount = "-3.50", currency = "EUR"),
        remittanceInformationUnstructured = remittance,
        creditorName = "Cafe",
    )

    /**
     * Berlin Group lets a bank omit both id fields. Two identical same-day rows must not collapse
     * onto one synthetic id, or importBatch's ON CONFLICT DO NOTHING silently drops the second.
     */
    @Test
    fun `importSince gives identical id-less rows on the same day distinct external ids`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchAccountTransactions(eqArg("ACC-1"), anyArg()))
            .thenReturn(bookedResponse(coffee(), coffee()))
        val module = newModule(client)

        val page = module.importSince(
            ctx = ctx(config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1"))),
            cursor = "2024-01-01",
        )

        assertEquals(2, page.transactions.size)
        assertEquals(2, page.transactions.map { it.externalId }.distinct().size)
    }

    @Test
    fun `synthetic external ids are stable across a re-sync of the same window`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchAccountTransactions(eqArg("ACC-1"), anyArg()))
            .thenReturn(bookedResponse(coffee(), coffee(), coffee(remittance = "Lunch")))
        val module = newModule(client)

        val first = module.importSince(
            ctx = ctx(config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1"))),
            cursor = "2024-01-01",
        )
        val second = module.importSince(
            ctx = ctx(config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1"))),
            cursor = "2024-01-01",
        )

        assertEquals(
            first.transactions.map { it.externalId },
            second.transactions.map { it.externalId },
        )
    }

    @Test
    fun `synthetic external ids differ when only the remittance text differs`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchAccountTransactions(eqArg("ACC-1"), anyArg()))
            .thenReturn(bookedResponse(coffee(), coffee(remittance = "Lunch")))
        val module = newModule(client)

        val page = module.importSince(
            ctx = ctx(config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1"))),
            cursor = "2024-01-01",
        )

        assertEquals(2, page.transactions.map { it.externalId }.distinct().size)
    }

    @Test
    fun `a bank-supplied transactionId still wins over the synthetic id`() {
        val client = mock(GoCardlessHttpClient::class.java)
        `when`(client.fetchAccountTransactions(eqArg("ACC-1"), anyArg())).thenReturn(
            bookedResponse(
                GoCardlessTransaction(
                    internalTransactionId = "INT-1",
                    transactionId = "T-1",
                    bookingDate = "2024-01-15",
                    transactionAmount = GoCardlessAmount(amount = "-3.50", currency = "EUR"),
                ),
            )
        )
        val module = newModule(client)

        val page = module.importSince(
            ctx = ctx(config = mapOf("country" to "DE", "accountIds" to listOf("ACC-1"))),
            cursor = "2024-01-01",
        )

        assertEquals("INT-1", page.transactions.single().externalId)
    }
}
