package beer.thierry.centsible.integrations.banking.gocardless

import beer.thierry.centsible.integrations.support.exchangeOrThrow
import beer.thierry.centsible.integrations.support.providerRetry
import beer.thierry.centsible.integrations.support.requireNonBlank
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class GoCardlessHttpClient(
    private val restClient: RestClient,
    private val secretId: String,
    private val secretKey: String,
) {

    private val log = LoggerFactory.getLogger(GoCardlessHttpClient::class.java)
    private val tokenLock = ReentrantLock()
    private val retry = providerRetry(PROVIDER)

    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var accessExpiresAt: Instant = Instant.EPOCH

    @Volatile
    private var refreshToken: String? = null

    @Volatile
    private var refreshExpiresAt: Instant = Instant.EPOCH

    fun listInstitutions(country: String): List<Institution> {
        requireNonBlank(country, "country")
        val uri = UriComponentsBuilder.fromUriString("/institutions/")
            .queryParam("country", country)
            .build(true)
            .toUriString()
        log.debug("GET /institutions provider={} country={}", PROVIDER, country)
        val raw: Array<InstitutionDto> = authedGet(uri, context = "institutions country=$country")
        log.info("Listed institutions provider={} country={} count={}", PROVIDER, country, raw.size)
        return raw.map { Institution(id = it.id, name = it.name, bic = it.bic, logo = it.logo) }
    }

    fun createEndUserAgreement(institutionId: String, maxHistoricalDays: Int): String {
        requireNonBlank(institutionId, "institutionId")
        require(maxHistoricalDays in 1..730) { "maxHistoricalDays out of range" }
        val body = mapOf(
            "institution_id" to institutionId,
            "max_historical_days" to maxHistoricalDays,
            "access_valid_for_days" to ACCESS_VALID_FOR_DAYS,
            "access_scope" to listOf("balances", "details", "transactions"),
        )
        log.debug("POST /agreements/enduser provider={} institutionId={} historicalDays={}", PROVIDER, institutionId, maxHistoricalDays)
        val resp: AgreementDto = authedPost("/agreements/enduser/", body, context = "agreements/enduser")
        log.info("End-user agreement created provider={} institutionId={} agreementId={}", PROVIDER, institutionId, resp.id)
        return resp.id
    }

    fun createRequisition(
        institutionId: String,
        agreementId: String,
        redirectUri: String,
        reference: String,
    ): RequisitionResponse {
        requireNonBlank(institutionId, "institutionId")
        requireNonBlank(agreementId, "agreementId")
        requireNonBlank(redirectUri, "redirectUri")
        requireNonBlank(reference, "reference")
        val body = mapOf(
            "institution_id" to institutionId,
            "agreement" to agreementId,
            "redirect" to redirectUri,
            "reference" to reference,
            "user_language" to "EN",
        )
        log.debug("POST /requisitions provider={} institutionId={} agreementId={}", PROVIDER, institutionId, agreementId)
        val resp: RequisitionDto = authedPost("/requisitions/", body, context = "requisitions")
        log.info("Requisition created provider={} requisitionId={} status={}", PROVIDER, resp.id, resp.status)
        return resp.toResponse()
    }

    fun fetchRequisition(requisitionId: String): RequisitionResponse {
        requireNonBlank(requisitionId, "requisitionId")
        log.debug("GET /requisitions/{} provider={}", requisitionId, PROVIDER)
        val resp: RequisitionDto = authedGet("/requisitions/{id}/", requisitionId, context = "requisitions/$requisitionId")
        log.info(
            "Fetched requisition provider={} requisitionId={} status={} accounts={}",
            PROVIDER, resp.id, resp.status, resp.accounts?.size ?: 0,
        )
        return resp.toResponse()
    }

    fun fetchAccountDetails(accountId: String): AccountDetails {
        requireNonBlank(accountId, "accountId")
        log.debug("GET /accounts/{}/details provider={}", accountId, PROVIDER)
        val resp: AccountDetailsEnvelope = authedGet("/accounts/{id}/details/", accountId, context = "accounts/$accountId/details")
        val a = resp.account
        return AccountDetails(
            iban = a?.iban,
            currency = a?.currency,
            ownerName = a?.ownerName,
            displayName = a?.name ?: a?.product ?: a?.ownerName,
        )
    }

    fun fetchAccountTransactions(accountId: String, dateFrom: LocalDate?): TransactionsResponse {
        requireNonBlank(accountId, "accountId")
        val builder = UriComponentsBuilder.fromUriString("/accounts/{id}/transactions/")
        if (dateFrom != null) builder.queryParam("date_from", dateFrom.toString())
        val uriTemplate = builder.build(false).toUriString()
        log.debug("GET /accounts/{}/transactions provider={} dateFrom={}", accountId, PROVIDER, dateFrom)
        val envelope: TransactionsEnvelope = authedGet(uriTemplate, accountId, context = "accounts/$accountId/transactions")
        val booked = envelope.transactions?.booked ?: emptyList()
        log.info(
            "Fetched transactions provider={} accountId={} bookedCount={} dateFrom={}",
            PROVIDER, accountId, booked.size, dateFrom,
        )
        return TransactionsResponse(booked = booked)
    }

    /**
     * Ensures a valid bearer token, then issues an authenticated GET and deserializes the body to
     * [T]. [uriVars] expand any `{...}` placeholders in [uri]. Collapses the ensureToken + retry +
     * bearer/accept boilerplate every read endpoint repeated.
     */
    private inline fun <reified T : Any> authedGet(uri: String, vararg uriVars: Any, context: String): T {
        val token = ensureToken()
        return retry.call {
            restClient.get()
                .uri(uri, *uriVars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .exchangeOrThrow(PROVIDER, context = context)
        }
    }

    /**
     * Ensures a valid bearer token, then issues an authenticated JSON POST of [body] and
     * deserializes the response to [T]. Counterpart to [authedGet] for write endpoints.
     */
    private inline fun <reified T : Any> authedPost(uri: String, body: Any, context: String): T {
        val token = ensureToken()
        return retry.call {
            restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchangeOrThrow(PROVIDER, context = context)
        }
    }

    private fun ensureToken(): String {
        val cached = accessToken
        if (cached != null && Instant.now().plusSeconds(TOKEN_SAFETY_WINDOW_SECONDS).isBefore(accessExpiresAt)) {
            return cached
        }
        tokenLock.withLock {
            val current = accessToken
            if (current != null && Instant.now().plusSeconds(TOKEN_SAFETY_WINDOW_SECONDS).isBefore(accessExpiresAt)) {
                return current
            }
            val refresh = refreshToken
            if (refresh != null && Instant.now().isBefore(refreshExpiresAt)) {
                try {
                    val refreshed = refreshAccessToken(refresh)
                    applyTokens(refreshed.access, refreshed.accessExpires, refreshToken, refreshExpiresAt.epochSecond - Instant.now().epochSecond)
                    log.info("Token refreshed provider={} expiresIn={}s", PROVIDER, refreshed.accessExpires)
                    return refreshed.access
                } catch (ex: Exception) {
                    log.warn("Refresh failed, falling back to fresh obtain provider={}", PROVIDER, ex)
                }
            }
            val obtained = try {
                obtainNewToken()
            } catch (ex: Exception) {
                log.error("Token obtain failed provider={}", PROVIDER, ex)
                throw ex
            }
            applyTokens(obtained.access, obtained.accessExpires, obtained.refresh, obtained.refreshExpires)
            log.info("Token obtained provider={} expiresIn={}s", PROVIDER, obtained.accessExpires)
            return obtained.access
        }
    }

    private fun applyTokens(access: String, accessExpiresIn: Long, refresh: String?, refreshExpiresIn: Long) {
        accessToken = access
        accessExpiresAt = Instant.now().plusSeconds(accessExpiresIn)
        if (refresh != null) {
            refreshToken = refresh
            refreshExpiresAt = Instant.now().plusSeconds(refreshExpiresIn)
        }
    }

    private fun obtainNewToken(): TokenResponse {
        val body = mapOf("secret_id" to secretId, "secret_key" to secretKey)
        return retry.call {
            restClient.post()
                .uri("/token/new/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchangeOrThrow(PROVIDER, context = "token/new")
        }
    }

    private fun refreshAccessToken(refresh: String): TokenResponse {
        val body = mapOf("refresh" to refresh)
        return retry.call {
            restClient.post()
                .uri("/token/refresh/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchangeOrThrow(PROVIDER, context = "token/refresh")
        }
    }

    private fun RequisitionDto.toResponse(): RequisitionResponse = RequisitionResponse(
        id = id,
        link = link ?: "",
        status = status ?: "",
        accounts = accounts ?: emptyList(),
    )

    companion object {
        const val PROVIDER = "GoCardless"
        private const val TOKEN_SAFETY_WINDOW_SECONDS = 60L
        private const val ACCESS_VALID_FOR_DAYS = 90
    }
}

data class Institution(
    val id: String,
    val name: String,
    val bic: String? = null,
    val logo: String? = null,
)

data class RequisitionResponse(
    val id: String,
    val link: String,
    val status: String,
    val accounts: List<String> = emptyList(),
)

data class AccountDetails(
    val iban: String? = null,
    val currency: String? = null,
    val ownerName: String? = null,
    val displayName: String? = null,
)

data class TransactionsResponse(
    val booked: List<GoCardlessTransaction> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoCardlessTransaction(
    @JsonProperty("transactionId") val transactionId: String? = null,
    @JsonProperty("internalTransactionId") val internalTransactionId: String? = null,
    @JsonProperty("bookingDate") val bookingDate: String? = null,
    @JsonProperty("bookingDateTime") val bookingDateTime: String? = null,
    @JsonProperty("valueDate") val valueDate: String? = null,
    @JsonProperty("transactionAmount") val transactionAmount: GoCardlessAmount,
    @JsonProperty("remittanceInformationUnstructured") val remittanceInformationUnstructured: String? = null,
    @JsonProperty("remittanceInformationStructured") val remittanceInformationStructured: String? = null,
    @JsonProperty("additionalInformation") val additionalInformation: String? = null,
    @JsonProperty("creditorName") val creditorName: String? = null,
    @JsonProperty("debtorName") val debtorName: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoCardlessAmount(
    @JsonProperty("amount") val amount: String,
    @JsonProperty("currency") val currency: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenResponse(
    @JsonProperty("access") val access: String,
    @JsonProperty("access_expires") val accessExpires: Long = 0,
    @JsonProperty("refresh") val refresh: String? = null,
    @JsonProperty("refresh_expires") val refreshExpires: Long = 0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstitutionDto(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("bic") val bic: String? = null,
    @JsonProperty("logo") val logo: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AgreementDto(
    @JsonProperty("id") val id: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RequisitionDto(
    @JsonProperty("id") val id: String,
    @JsonProperty("link") val link: String? = null,
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("accounts") val accounts: List<String>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccountDetailsEnvelope(
    @JsonProperty("account") val account: AccountInfoDto? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccountInfoDto(
    @JsonProperty("iban") val iban: String? = null,
    @JsonProperty("currency") val currency: String? = null,
    @JsonProperty("ownerName") val ownerName: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("product") val product: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionsEnvelope(
    @JsonProperty("transactions") val transactions: TransactionsBlock? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionsBlock(
    @JsonProperty("booked") val booked: List<GoCardlessTransaction> = emptyList(),
    @JsonProperty("pending") val pending: List<GoCardlessTransaction> = emptyList(),
)
