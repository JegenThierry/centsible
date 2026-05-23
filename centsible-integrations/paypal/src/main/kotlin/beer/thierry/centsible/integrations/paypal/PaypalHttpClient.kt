package beer.thierry.centsible.integrations.paypal

import beer.thierry.centsible.integrations.support.exchangeOrThrow
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64

class PaypalHttpClient(private val restClient: RestClient) {

    private val log = LoggerFactory.getLogger(PaypalHttpClient::class.java)

    fun obtainAccessToken(environment: String, clientId: String, clientSecret: String): TokenResponse {
        require(clientId.isNotBlank()) { "clientId must not be blank" }
        require(clientSecret.isNotBlank()) { "clientSecret must not be blank" }

        val basic = Base64.getEncoder()
            .encodeToString("$clientId:$clientSecret".toByteArray(Charsets.UTF_8))
        val form = LinkedMultiValueMap<String, String>().apply { add("grant_type", "client_credentials") }

        return restClient.post()
            .uri("${baseFor(environment)}/v1/oauth2/token")
            .header(HttpHeaders.AUTHORIZATION, "Basic $basic")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .body(form)
            .exchangeOrThrow(PROVIDER, context = "oauth2/token")
    }

    fun fetchUserInfo(environment: String, accessToken: String): UserInfoResponse {
        require(accessToken.isNotBlank()) { "accessToken must not be blank" }

        return restClient.get()
            .uri("${baseFor(environment)}/v1/identity/oauth2/userinfo?schema=paypalv1.1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchangeOrThrow(PROVIDER, context = "identity/userinfo")
    }

    fun fetchTransactions(
        environment: String,
        accessToken: String,
        startDate: Instant,
        endDate: Instant,
        page: Int,
    ): TransactionsResponse {
        require(accessToken.isNotBlank()) { "accessToken must not be blank" }
        require(page >= 1) { "page must be >= 1" }

        val uri = UriComponentsBuilder.fromUriString("${baseFor(environment)}/v1/reporting/transactions")
            .queryParam("start_date", ISO_OFFSET.format(startDate.atOffset(ZoneOffset.UTC)))
            .queryParam("end_date", ISO_OFFSET.format(endDate.atOffset(ZoneOffset.UTC)))
            .queryParam("fields", "all")
            .queryParam("page_size", 500)
            .queryParam("page", page)
            .build(true)
            .toUri()

        return restClient.get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .exchangeOrThrow(PROVIDER, context = "reporting/transactions page=$page")
    }

    private fun baseFor(env: String): String = when (env.lowercase()) {
        "sandbox" -> "https://api-m.sandbox.paypal.com"
        "live" -> "https://api-m.paypal.com"
        else -> {
            log.warn("Unknown PayPal environment '{}', defaulting to live", env)
            "https://api-m.paypal.com"
        }
    }

    companion object {
        const val PROVIDER = "PayPal"
        private val ISO_OFFSET: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String? = null,
    @JsonProperty("expires_in") val expiresIn: Long? = null,
    @JsonProperty("scope") val scope: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserInfoResponse(
    @JsonProperty("user_id") val userId: String,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("email") val email: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionsResponse(
    @JsonProperty("transaction_details") val transactionDetails: List<TransactionDetail> = emptyList(),
    @JsonProperty("page") val page: Int = 1,
    @JsonProperty("total_pages") val totalPages: Int = 1,
    @JsonProperty("total_items") val totalItems: Int = 0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionDetail(
    @JsonProperty("transaction_info") val transactionInfo: TransactionInfo,
    @JsonProperty("payer_info") val payerInfo: PayerInfo? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionInfo(
    @JsonProperty("transaction_id") val transactionId: String,
    @JsonProperty("transaction_event_code") val transactionEventCode: String? = null,
    @JsonProperty("transaction_initiation_date") val transactionInitiationDate: String,
    @JsonProperty("transaction_amount") val transactionAmount: MoneyAmount? = null,
    @JsonProperty("transaction_subject") val transactionSubject: String? = null,
    @JsonProperty("transaction_note") val transactionNote: String? = null,
    @JsonProperty("invoice_id") val invoiceId: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MoneyAmount(
    @JsonProperty("value") val value: String,
    @JsonProperty("currency_code") val currencyCode: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PayerInfo(
    @JsonProperty("email_address") val emailAddress: String? = null,
    @JsonProperty("payer_name") val payerName: PayerName? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PayerName(
    @JsonProperty("alternate_full_name") val alternateFullName: String? = null,
    @JsonProperty("given_name") val givenName: String? = null,
    @JsonProperty("surname") val surname: String? = null,
)
