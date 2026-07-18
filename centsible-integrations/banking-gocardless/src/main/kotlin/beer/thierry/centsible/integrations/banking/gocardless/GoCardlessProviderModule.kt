package beer.thierry.centsible.integrations.banking.gocardless

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.model.integrations.Capability
import beer.thierry.centsible.api.model.integrations.ConfigField
import beer.thierry.centsible.api.model.integrations.ExternalAccountDTO
import beer.thierry.centsible.api.model.integrations.FieldType
import beer.thierry.centsible.api.model.integrations.ImportedTransactionDTO
import beer.thierry.centsible.api.model.integrations.OAuthCallbackRequest
import beer.thierry.centsible.api.model.integrations.OAuthCredentialEnvelope
import beer.thierry.centsible.api.model.integrations.OAuthStartRequest
import beer.thierry.centsible.api.model.integrations.ProviderContext
import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.model.integrations.RemoteOptionsRequest
import beer.thierry.centsible.api.model.integrations.SelectOption
import beer.thierry.centsible.api.model.integrations.TransactionImportPage
import beer.thierry.centsible.api.services.integrations.IAccountProvider
import beer.thierry.centsible.api.services.integrations.IOAuthFlowProvider
import beer.thierry.centsible.api.services.integrations.IRemoteOptionsProvider
import beer.thierry.centsible.api.services.integrations.ITransactionImporter
import beer.thierry.centsible.api.services.integrations.OAuthAuthorizationStart
import beer.thierry.centsible.api.services.integrations.OAuthCallbackResult
import beer.thierry.centsible.api.services.integrations.ProviderModule
import beer.thierry.centsible.integrations.banking.gocardless.GoCardlessHttpClient.Companion.PROVIDER
import beer.thierry.centsible.integrations.support.firstNonBlank
import beer.thierry.centsible.integrations.support.nonBlankString
import beer.thierry.centsible.integrations.support.parseDateOnlyAtUtc
import beer.thierry.centsible.integrations.support.requiredString
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.HexFormat

class GoCardlessProviderModule(
    private val client: GoCardlessHttpClient,
    override val minSyncIntervalSeconds: Long,
) : ProviderModule, IAccountProvider, ITransactionImporter, IOAuthFlowProvider, IRemoteOptionsProvider {

    private val log = LoggerFactory.getLogger(GoCardlessProviderModule::class.java)

    override val descriptor = ProviderDescriptor(
        key = "banking-gocardless",
        displayName = "EU Banks (GoCardless)",
        description = "Connect any EU/EEA bank account under PSD2 via GoCardless Bank Account Data. " +
            "The operator must configure GoCardless secrets in env first; end users only authorize bank consent.",
        authType = AuthType.OAUTH2,
        capabilities = setOf(Capability.ACCOUNTS, Capability.TRANSACTIONS, Capability.OAUTH_FLOW),
        configFields = listOf(
            ConfigField(
                name = "country",
                label = "Country",
                type = FieldType.SELECT,
                required = true,
                options = EEA_COUNTRIES,
                helpText = "EEA country where your bank is located.",
            ),
            ConfigField(
                name = "institutionId",
                label = "Bank",
                type = FieldType.SELECT_REMOTE,
                required = true,
                dependsOn = listOf("country"),
                helpText = "Search for your bank. Loads after you pick a country.",
            ),
            ConfigField(
                name = "historicalDays",
                label = "Historical days",
                type = FieldType.NUMBER,
                required = false,
                placeholder = "90",
                helpText = "Days of historical transactions to import (max varies by bank, typically 90-730).",
            ),
            ConfigField(
                name = "oauthLaunch",
                label = "Connect to bank",
                type = FieldType.OAUTH_LAUNCH,
                required = false,
                helpText = "After saving, click here to authorize at your bank.",
            ),
        ),
    )

    override fun testConnection(ctx: ProviderContext) {
        val country = ctx.config.requiredString("country", "country")
        val institutionId = ctx.config.requiredString("institutionId", "institutionId")
        if (country !in EEA_COUNTRY_CODES) {
            throw IllegalArgumentException("Unsupported country code: $country")
        }
        if (institutionId.length < 2) {
            throw IllegalArgumentException("institutionId looks invalid")
        }
    }

    override fun fetchOptions(ctx: ProviderContext, request: RemoteOptionsRequest): List<SelectOption> {
        if (request.fieldName != "institutionId") return emptyList()
        val country = request.values.nonBlankString("country") ?: return emptyList()
        val query = request.query?.trim()?.lowercase()
        val institutions = try {
            client.listInstitutions(country)
        } catch (ex: Exception) {
            log.warn("Institution lookup failed provider={} country={}", PROVIDER, country, ex)
            return emptyList()
        }
        val filtered = if (query.isNullOrBlank()) {
            institutions
        } else {
            institutions.filter {
                it.name.lowercase().contains(query) || (it.bic?.lowercase()?.contains(query) == true)
            }
        }
        return filtered.take(MAX_REMOTE_OPTIONS).map { inst ->
            SelectOption(
                value = inst.id,
                label = inst.name + (inst.bic?.let { " ($it)" } ?: ""),
            )
        }
    }

    /**
     * The callback only echoes our reference (`ref=state`); to recover the requisitionId at
     * callback time we persist it into the connection's config via configPatch, which the
     * orchestrator writes before the user is redirected.
     */
    override fun buildAuthorizationUrl(request: OAuthStartRequest): OAuthAuthorizationStart {
        val institutionId = request.config.requiredString("institutionId", "institutionId")
        val historicalDays = parseHistoricalDays(request.config["historicalDays"])
        log.info(
            "OAuth begin provider={} userId={} connectionId={} institutionId={}",
            PROVIDER, request.userId, request.connectionId, institutionId,
        )
        val agreementId = client.createEndUserAgreement(institutionId, historicalDays)
        val requisition = client.createRequisition(
            institutionId = institutionId,
            agreementId = agreementId,
            redirectUri = request.redirectUri,
            reference = request.state,
        )
        return OAuthAuthorizationStart(
            authorizationUrl = requisition.link,
            configPatch = mapOf(
                "pendingRequisitionId" to requisition.id,
                "pendingAgreementId" to agreementId,
            ),
        )
    }

    override fun completeAuthorization(request: OAuthCallbackRequest): OAuthCallbackResult {
        val requisitionId = request.config.nonBlankString("pendingRequisitionId")
            ?: run {
                log.warn(
                    "OAuth callback for connection with no pending requisition provider={} userId={} connectionId={}",
                    PROVIDER, request.userId, request.connectionId,
                )
                throw IllegalArgumentException("No pending requisition found for this connection")
            }
        val requisition = client.fetchRequisition(requisitionId)
        if (requisition.status !in CONSENT_OK_STATUSES) {
            log.warn(
                "OAuth consent not granted provider={} userId={} connectionId={} requisitionId={} status={}",
                PROVIDER, request.userId, request.connectionId, requisitionId, requisition.status,
            )
            throw IllegalStateException("Bank consent not granted (status=${requisition.status})")
        }
        log.info(
            "OAuth complete provider={} userId={} connectionId={} requisitionId={} accounts={}",
            PROVIDER, request.userId, request.connectionId, requisitionId, requisition.accounts.size,
        )
        val expiresAt = Instant.now().plusSeconds(CONSENT_LIFETIME_SECONDS)
        val envelope = OAuthCredentialEnvelope(
            accessToken = "delegated",
            expiresAt = expiresAt,
        )
        return OAuthCallbackResult(
            envelope = envelope,
            configPatch = mapOf(
                "requisitionId" to requisitionId,
                "accountIds" to requisition.accounts,
                "consentExpiresAt" to expiresAt.toString(),
                "pendingRequisitionId" to null,
                "pendingAgreementId" to null,
            ),
        )
    }

    override fun refreshAccessToken(ctx: ProviderContext): OAuthCredentialEnvelope {
        val existing = OAuthCredentialEnvelope.from(ctx.credentials)
            ?: throw IllegalStateException("No OAuth envelope in credentials")
        val consentExpiresAt = ctx.config["consentExpiresAt"]?.toString()?.let {
            try {
                Instant.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        }
        if (consentExpiresAt != null && Instant.now().isAfter(consentExpiresAt)) {
            log.warn(
                "PSD2 consent expired provider={} userId={} connectionId={} expiredAt={}",
                PROVIDER, ctx.userId, ctx.connectionId, consentExpiresAt,
            )
            throw IllegalStateException("PSD2 consent expired; user must re-authorize")
        }
        return existing
    }

    override fun listExternalAccounts(ctx: ProviderContext): List<ExternalAccountDTO> {
        val accountIds = extractAccountIds(ctx.config["accountIds"])
        if (accountIds.isEmpty()) return emptyList()
        return accountIds.map { id ->
            val details = try {
                client.fetchAccountDetails(id)
            } catch (ex: Exception) {
                log.warn(
                    "Account-details fetch failed provider={} userId={} connectionId={} accountId={}",
                    PROVIDER, ctx.userId, ctx.connectionId, id, ex,
                )
                AccountDetails()
            }
            ExternalAccountDTO(
                externalId = id,
                name = details.displayName ?: details.iban ?: id,
                type = "bank",
                currency = details.currency,
                balance = null,
            )
        }
    }

    override fun importSince(ctx: ProviderContext, cursor: String?): TransactionImportPage {
        val accountIds = extractAccountIds(ctx.config["accountIds"])
        if (accountIds.isEmpty()) {
            log.warn(
                "Sync skipped, no accounts configured provider={} userId={} connectionId={}",
                PROVIDER, ctx.userId, ctx.connectionId,
            )
            return TransactionImportPage(transactions = emptyList(), nextCursor = LocalDate.now().toString())
        }

        val started = System.currentTimeMillis()
        val dateFrom = resolveDateFrom(cursor, ctx.config["historicalDays"])
        log.info(
            "Sync begin provider={} userId={} connectionId={} accounts={} dateFrom={}",
            PROVIDER, ctx.userId, ctx.connectionId, accountIds.size, dateFrom,
        )
        val all = mutableListOf<ImportedTransactionDTO>()
        val syntheticOccurrences = mutableMapOf<String, Int>()
        var skippedRows = 0
        var failedAccounts = 0
        for (accountId in accountIds) {
            val response = try {
                client.fetchAccountTransactions(accountId, dateFrom)
            } catch (ex: Exception) {
                failedAccounts++
                log.warn(
                    "Transactions fetch failed provider={} userId={} connectionId={} accountId={}",
                    PROVIDER, ctx.userId, ctx.connectionId, accountId, ex,
                )
                continue
            }
            for (tx in response.booked) {
                val occurredAt = parseOccurredAt(tx)
                if (occurredAt == null) {
                    skippedRows++
                    continue
                }
                val amount = try {
                    BigDecimal(tx.transactionAmount.amount)
                } catch (_: NumberFormatException) {
                    skippedRows++
                    continue
                }
                val description = firstNonBlank(
                    tx.remittanceInformationUnstructured,
                    tx.remittanceInformationStructured,
                    tx.additionalInformation,
                ) ?: ""
                val externalId = tx.internalTransactionId
                    ?: tx.transactionId
                    ?: syntheticExternalId(accountId, tx, syntheticOccurrences)
                val counterparty = firstNonBlank(tx.creditorName, tx.debtorName)
                all += ImportedTransactionDTO(
                    externalId = externalId,
                    externalAccountId = accountId,
                    amount = amount,
                    currency = tx.transactionAmount.currency,
                    description = description,
                    occurredAt = occurredAt,
                    counterparty = counterparty,
                    metadata = emptyMap(),
                )
            }
        }
        if (skippedRows > 0) {
            log.warn(
                "Sync rows skipped (unparseable date/amount) provider={} userId={} connectionId={} skipped={}",
                PROVIDER, ctx.userId, ctx.connectionId, skippedRows,
            )
        }
        if (failedAccounts > 0) {
            log.warn(
                "Sync incomplete provider={} userId={} connectionId={} failedAccounts={} of {} fetched={} dateFrom={} elapsedMs={}",
                PROVIDER, ctx.userId, ctx.connectionId, failedAccounts, accountIds.size, all.size, dateFrom,
                System.currentTimeMillis() - started,
            )
            throw IllegalStateException(
                "Transactions could not be fetched for $failedAccounts of ${accountIds.size} account(s); " +
                    "not advancing the sync cursor so the window from $dateFrom is retried instead of skipped"
            )
        }
        log.info(
            "Sync complete provider={} userId={} connectionId={} processed={} skippedRows={} elapsedMs={}",
            PROVIDER, ctx.userId, ctx.connectionId, all.size, skippedRows,
            System.currentTimeMillis() - started,
        )
        return TransactionImportPage(transactions = all, nextCursor = LocalDate.now().toString())
    }

    private fun parseHistoricalDays(raw: Any?): Int {
        val parsed = when (raw) {
            null -> DEFAULT_HISTORICAL_DAYS
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull() ?: DEFAULT_HISTORICAL_DAYS
            else -> DEFAULT_HISTORICAL_DAYS
        }
        return parsed.coerceIn(1, 730)
    }

    private fun resolveDateFrom(cursor: String?, historicalDaysRaw: Any?): LocalDate {
        if (!cursor.isNullOrBlank()) {
            try {
                return LocalDate.parse(cursor).minusDays(REFETCH_OVERLAP_DAYS)
            } catch (_: DateTimeParseException) {
            }
        }
        val days = parseHistoricalDays(historicalDaysRaw)
        return LocalDate.now().minusDays(days.toLong())
    }

    /**
     * Berlin Group permits a bank to send neither `internalTransactionId` nor `transactionId`, so
     * a synthetic id has to stand in. It must distinguish two genuinely different rows (two 3.50
     * coffees on one day) yet stay byte-identical across re-syncs, because TransactionService
     * hashes it into the import_hash that dedups the batch — an unstable id duplicates rows, a
     * colliding one silently drops them.
     *
     * The fingerprint covers every field the bank gives us; rows that still collide are
     * indistinguishable, so they are numbered by their order of arrival. That order is stable
     * because [occurrences] is keyed by the fingerprint (an unrelated row on the same day cannot
     * shift a counter) and because every row sharing a fingerprint also shares a booking date:
     * date_from windows are date-granular, so such rows are always fetched together, never split
     * across two syncs.
     */
    private fun syntheticExternalId(
        accountId: String,
        tx: GoCardlessTransaction,
        occurrences: MutableMap<String, Int>,
    ): String {
        val fingerprint = sha256Hex(
            listOf(
                accountId,
                tx.bookingDate,
                tx.bookingDateTime,
                tx.valueDate,
                tx.transactionAmount.amount,
                tx.transactionAmount.currency,
                tx.remittanceInformationUnstructured,
                tx.remittanceInformationStructured,
                tx.additionalInformation,
                tx.creditorName,
                tx.debtorName,
            ).joinToString(FINGERPRINT_SEPARATOR) { it.orEmpty() }
        )
        val occurrence = occurrences.merge(fingerprint, 1, Int::plus)!!
        return "$accountId:$fingerprint:$occurrence"
    }

    private fun sha256Hex(payload: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(digest)
    }

    private fun parseOccurredAt(tx: GoCardlessTransaction): OffsetDateTime? {
        tx.bookingDateTime?.let {
            try {
                return OffsetDateTime.parse(it)
            } catch (_: DateTimeParseException) {
            }
        }
        val dateStr = tx.bookingDate ?: tx.valueDate ?: return null
        return parseDateOnlyAtUtc(dateStr)
    }

    private fun extractAccountIds(raw: Any?): List<String> = when (raw) {
        null -> emptyList()
        is Collection<*> -> raw.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
        is String -> if (raw.isBlank()) emptyList() else listOf(raw)
        else -> emptyList()
    }

    companion object {
        private const val DEFAULT_HISTORICAL_DAYS = 90
        private const val REFETCH_OVERLAP_DAYS = 7L
        private const val MAX_REMOTE_OPTIONS = 50
        private const val CONSENT_LIFETIME_SECONDS = 90L * 86400L
        private val CONSENT_OK_STATUSES = setOf("LN", "GC")

        private const val FINGERPRINT_SEPARATOR = "\u001F"
        private val EEA_COUNTRY_CODES = setOf(
            "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI",
            "FR", "GB", "GR", "HR", "HU", "IE", "IS", "IT", "LI", "LT",
            "LU", "LV", "MT", "NL", "NO", "PL", "PT", "RO", "SE", "SI", "SK",
        )
        private val COUNTRY_LABELS = mapOf(
            "AT" to "Austria", "BE" to "Belgium", "BG" to "Bulgaria", "CY" to "Cyprus",
            "CZ" to "Czechia", "DE" to "Germany", "DK" to "Denmark", "EE" to "Estonia",
            "ES" to "Spain", "FI" to "Finland", "FR" to "France", "GB" to "United Kingdom",
            "GR" to "Greece", "HR" to "Croatia", "HU" to "Hungary", "IE" to "Ireland",
            "IS" to "Iceland", "IT" to "Italy", "LI" to "Liechtenstein", "LT" to "Lithuania",
            "LU" to "Luxembourg", "LV" to "Latvia", "MT" to "Malta", "NL" to "Netherlands",
            "NO" to "Norway", "PL" to "Poland", "PT" to "Portugal", "RO" to "Romania",
            "SE" to "Sweden", "SI" to "Slovenia", "SK" to "Slovakia",
        )
        private val EEA_COUNTRIES: List<SelectOption> = EEA_COUNTRY_CODES.sorted().map { code ->
            SelectOption(value = code, label = "${COUNTRY_LABELS[code] ?: code} ($code)")
        }
    }
}
