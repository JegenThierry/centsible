package beer.thierry.centsible.integrations.paypal

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.model.integrations.Capability
import beer.thierry.centsible.api.model.integrations.ConfigField
import beer.thierry.centsible.api.model.integrations.ExternalAccountDTO
import beer.thierry.centsible.api.model.integrations.FieldType
import beer.thierry.centsible.api.model.integrations.ImportedTransactionDTO
import beer.thierry.centsible.api.model.integrations.ProviderContext
import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.model.integrations.SelectOption
import beer.thierry.centsible.api.model.integrations.TransactionImportPage
import beer.thierry.centsible.api.services.integrations.IAccountProvider
import beer.thierry.centsible.api.services.integrations.ITransactionImporter
import beer.thierry.centsible.api.services.integrations.ProviderModule
import beer.thierry.centsible.integrations.support.firstNonBlank
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClientResponseException
import java.io.IOException
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

class PaypalProviderModule(
    private val client: PaypalHttpClient,
) : ProviderModule, IAccountProvider, ITransactionImporter {

    private val log = LoggerFactory.getLogger(PaypalProviderModule::class.java)

    override val descriptor = ProviderDescriptor(
        key = "paypal",
        displayName = "PayPal",
        description = "Import PayPal Transactions via your REST API app credentials (Personal or Business). " +
            "Create a REST app at developer.paypal.com to obtain the client ID and secret.",
        authType = AuthType.API_KEY,
        capabilities = setOf(Capability.ACCOUNTS, Capability.TRANSACTIONS),
        configFields = listOf(
            ConfigField(
                name = "clientId",
                label = "Client ID",
                type = FieldType.STRING,
                required = true,
                placeholder = "AYj4...your-app-client-id...",
                helpText = "From the PayPal developer dashboard.",
            ),
            ConfigField(
                name = "clientSecret",
                label = "Client secret",
                type = FieldType.STRING,
                required = true,
                secret = true,
                placeholder = "EFk2...your-app-secret...",
            ),
            ConfigField(
                name = "environment",
                label = "Environment",
                type = FieldType.SELECT,
                required = true,
                options = listOf(
                    SelectOption(value = "live", label = "Live"),
                    SelectOption(value = "sandbox", label = "Sandbox"),
                ),
                helpText = "Sandbox uses developer-mode test data.",
            ),
            ConfigField(
                name = "startDate",
                label = "Start date",
                type = FieldType.STRING,
                required = false,
                placeholder = "2024-01-01",
                helpText = "ISO date for the first import. Defaults to 90 days ago (PayPal's hard maximum window per call).",
            ),
        ),
    )

    override fun testConnection(ctx: ProviderContext) {
        val (clientId, clientSecret, environment) = readCredentials(ctx)
        log.info(
            "Test connection provider=PayPal userId={} connectionId={} env={}",
            ctx.userId, ctx.connectionId, environment,
        )
        client.obtainAccessToken(environment, clientId, clientSecret)
    }

    override fun listExternalAccounts(ctx: ProviderContext): List<ExternalAccountDTO> {
        // The userinfo endpoint requires "openid profile email" scopes on the merchant's REST
        // app. Older / Personal apps may lack them, in which case the call 403s. Best-effort
        // enrichment for the display name; the account id stays a stable constant. Catch only
        // expected HTTP/IO errors — other Throwables propagate so real bugs are visible.
        val displayName = try {
            val (clientId, clientSecret, environment) = readCredentials(ctx)
            val token = client.obtainAccessToken(environment, clientId, clientSecret)
            val userInfo = client.fetchUserInfo(environment, token.accessToken)
            userInfo.email?.takeIf { it.isNotBlank() }
                ?: userInfo.name?.takeIf { it.isNotBlank() }
                ?: "PayPal"
        } catch (e: RestClientResponseException) {
            log.warn(
                "PayPal userinfo enrichment failed (HTTP {}); falling back to default display name. " +
                    "Confirm the REST app has the 'openid profile email' scopes enabled.",
                e.statusCode.value(),
            )
            "PayPal"
        } catch (e: IOException) {
            log.warn("PayPal userinfo network failure: {}; using default display name", e.javaClass.simpleName)
            "PayPal"
        }
        return listOf(
            ExternalAccountDTO(
                externalId = DEFAULT_ACCOUNT_ID,
                name = displayName,
                type = "paypal",
                currency = null,
                balance = null,
            ),
        )
    }

    override fun importSince(ctx: ProviderContext, cursor: String?): TransactionImportPage {
        val (clientId, clientSecret, environment) = readCredentials(ctx)
        val started = System.currentTimeMillis()
        val token = try {
            client.obtainAccessToken(environment, clientId, clientSecret)
        } catch (ex: Exception) {
            log.error(
                "Sync failed at token obtain provider=PayPal userId={} connectionId={} env={}",
                ctx.userId, ctx.connectionId, environment, ex,
            )
            throw ex
        }

        val now = Instant.now()
        val endDate = now.minusSeconds(SAFETY_MARGIN_SECONDS)
        val configuredStart = (ctx.config["startDate"] as? String)?.takeIf { it.isNotBlank() }
        val startDate = resolveStartDate(cursor, configuredStart, now)

        if (!startDate.isBefore(endDate)) {
            log.info(
                "Sync skipped, start>=end provider=PayPal userId={} connectionId={} startDate={} endDate={}",
                ctx.userId, ctx.connectionId, startDate, endDate,
            )
            return TransactionImportPage(transactions = emptyList(), nextCursor = endDate.toString())
        }

        log.info(
            "Sync begin provider=PayPal userId={} connectionId={} env={} startDate={} endDate={}",
            ctx.userId, ctx.connectionId, environment, startDate, endDate,
        )

        val accumulated = mutableListOf<ImportedTransactionDTO>()
        var chunkStart = startDate
        try {
            while (chunkStart.isBefore(endDate)) {
                val chunkEnd = minOf(chunkStart.plus(MAX_WINDOW), endDate)
                var page = 1
                while (true) {
                    val response = client.fetchTransactions(environment, token.accessToken, chunkStart, chunkEnd, page)
                    response.transactionDetails.forEach { detail ->
                        mapTransaction(detail)?.let(accumulated::add)
                    }
                    if (page >= response.totalPages) break
                    page += 1
                }
                chunkStart = chunkEnd
            }
        } catch (ex: Exception) {
            log.error(
                "Sync failed provider=PayPal userId={} connectionId={} processedBeforeFailure={}",
                ctx.userId, ctx.connectionId, accumulated.size, ex,
            )
            throw ex
        }

        log.info(
            "Sync complete provider=PayPal userId={} connectionId={} processed={} elapsedMs={}",
            ctx.userId, ctx.connectionId, accumulated.size, System.currentTimeMillis() - started,
        )
        return TransactionImportPage(transactions = accumulated, nextCursor = endDate.toString())
    }

    private fun mapTransaction(detail: TransactionDetail): ImportedTransactionDTO? {
        val info = detail.transactionInfo
        val amountRaw = info.transactionAmount?.value ?: return null
        val amount = runCatching { BigDecimal(amountRaw) }
            .getOrElse {
                log.warn("Skipping PayPal transaction {} due to unparseable amount", info.transactionId)
                return null
            }

        val occurredAt = parsePaypalInstant(info.transactionInitiationDate)
            ?: run {
                log.warn("Skipping PayPal transaction {} due to unparseable date '{}'",
                    info.transactionId, info.transactionInitiationDate)
                return null
            }

        val description = firstNonBlank(
            info.transactionSubject,
            info.transactionNote,
            info.invoiceId,
        ) ?: "PayPal ${info.transactionEventCode ?: "transaction"}"

        val counterparty = detail.payerInfo?.payerName?.alternateFullName?.takeIf { it.isNotBlank() }
            ?: detail.payerInfo?.emailAddress?.takeIf { it.isNotBlank() }

        return ImportedTransactionDTO(
            externalId = info.transactionId,
            externalAccountId = DEFAULT_ACCOUNT_ID,
            amount = amount,
            currency = info.transactionAmount.currencyCode,
            description = description,
            occurredAt = occurredAt,
            counterparty = counterparty,
            metadata = emptyMap(),
        )
    }

    private fun resolveStartDate(cursor: String?, configuredStart: String?, now: Instant): Instant {
        if (!cursor.isNullOrBlank()) {
            runCatching { Instant.parse(cursor) }
                .onSuccess { return it }
                .onFailure { log.warn("Invalid PayPal cursor, falling back to configured start") }
        }
        if (configuredStart != null) {
            try {
                return LocalDate.parse(configuredStart).atStartOfDay(ZoneOffset.UTC).toInstant()
            } catch (ex: DateTimeParseException) {
                log.warn("Invalid PayPal startDate '{}', falling back to default window", configuredStart)
            }
        }
        return now.minus(DEFAULT_LOOKBACK)
    }

    /**
     * Resolves PayPal credentials from the provider context.
     *
     * Enforces the secrets-handling invariant: fields marked `secret = true` in the descriptor
     * (here, `clientSecret`) MUST come from the encrypted credentials map only. Falling back to
     * `ctx.config` would silently accept (and rely on) a secret stored in plaintext JSONB,
     * defeating the cipher. Non-secret fields (`clientId`, `environment`) live in `ctx.config`.
     */
    private fun readCredentials(ctx: ProviderContext): Triple<String, String, String> {
        val clientId = (ctx.config["clientId"] as? String)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("PayPal clientId is required")
        val clientSecret = ctx.credentials["clientSecret"]?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("PayPal clientSecret is missing from encrypted credentials")
        val environment = (ctx.config["environment"] as? String)?.takeIf { it.isNotBlank() }
            ?: "live"
        return Triple(clientId, clientSecret, environment)
    }

    /**
     * PayPal's Reporting API returns `transaction_initiation_date` in mixed offset formats —
     * '+0000' (no colon), '+00:00', and 'Z' all occur across endpoints and SDK versions. The
     * default ISO_OFFSET_DATE_TIME parser rejects '+0000', so without this dropped transactions
     * were the silent failure mode of every PayPal sync.
     */
    private fun parsePaypalInstant(value: String): OffsetDateTime? {
        try {
            return OffsetDateTime.parse(value, PAYPAL_DATE_FORMATTER)
        } catch (_: DateTimeParseException) {
        }
        return try {
            LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    companion object {
        private const val DEFAULT_ACCOUNT_ID = "paypal:default"
        private const val SAFETY_MARGIN_SECONDS = 300L
        private val DEFAULT_LOOKBACK: Duration = Duration.of(90, ChronoUnit.DAYS)

        /** PayPal Reporting API rejects ranges wider than 31 days per call; importSince chunks accordingly. */
        private val MAX_WINDOW: Duration = Duration.of(31, ChronoUnit.DAYS)

        private val PAYPAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendOffset("+HHMM", "Z")
            .toFormatter()
            .let { compact ->
                DateTimeFormatterBuilder()
                    .appendOptional(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .appendOptional(compact)
                    .toFormatter()
            }
    }
}
