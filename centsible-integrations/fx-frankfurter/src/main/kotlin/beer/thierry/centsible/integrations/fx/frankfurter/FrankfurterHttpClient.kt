package beer.thierry.centsible.integrations.fx.frankfurter

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.services.integrations.ProviderRate
import beer.thierry.centsible.integrations.support.exchangeOrThrow
import beer.thierry.centsible.integrations.support.providerRetry
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.time.LocalDate

class FrankfurterHttpClient(
    private val restClient: RestClient,
    private val apiBase: String,
) {
    private val log = LoggerFactory.getLogger(FrankfurterHttpClient::class.java)
    private val retry = providerRetry(PROVIDER)

    fun fetchRate(base: Currency, quote: Currency, date: LocalDate): ProviderRate {
        val uri = UriComponentsBuilder.fromUriString("$apiBase/v2/rates")
            .queryParam("date", date.toString())
            .queryParam("base", base.name)
            .queryParam("quotes", quote.name)
            .build(true)
            .toUri()

        log.debug("GET /v2/rates provider={} base={} quote={} date={}", PROVIDER, base, quote, date)
        val rates = retry.call {
            restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeOrThrow<Array<FrankfurterRate>>(PROVIDER, context = "rates $base->$quote @$date")
        }

        val match = rates.firstOrNull { it.rate != null }
            ?: throw IllegalStateException("Frankfurter returned no rate for $base->$quote @$date")
        return ProviderRate(rate = match.rate!!, rateDate = LocalDate.parse(match.date))
    }

    companion object {
        const val PROVIDER = "Frankfurter"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FrankfurterRate(
    @JsonProperty("date") val date: String,
    @JsonProperty("base") val base: String? = null,
    @JsonProperty("quote") val quote: String? = null,
    @JsonProperty("rate") val rate: BigDecimal? = null,
)
