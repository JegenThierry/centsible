package beer.thierry.centsible.integrations.fx.frankfurter

import beer.thierry.centsible.api.services.integrations.IExchangeRateProvider
import beer.thierry.centsible.integrations.support.timeoutRequestFactory
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@ConditionalOnProperty(value = ["fx.frankfurter.enabled"], havingValue = "true", matchIfMissing = true)
class FrankfurterFxConfig(
    @param:Value("\${fx.frankfurter.api-base:https://api.frankfurter.dev}") private val apiBase: String,
    @param:Value("\${integrations.http.connect-timeout-ms:10000}") private val connectTimeoutMs: Long,
    @param:Value("\${integrations.http.read-timeout-ms:30000}") private val readTimeoutMs: Long,
) {

    private val log = LoggerFactory.getLogger(FrankfurterFxConfig::class.java)

    @PostConstruct
    fun announce() {
        log.info("FX rate provider Frankfurter enabled (apiBase={})", apiBase)
    }

    @Bean
    fun frankfurterHttpClient(): FrankfurterHttpClient = FrankfurterHttpClient(
        RestClient.builder()
            .requestFactory(timeoutRequestFactory(Duration.ofMillis(connectTimeoutMs), Duration.ofMillis(readTimeoutMs)))
            .build(),
        apiBase,
    )

    @Bean
    fun frankfurterExchangeRateProvider(client: FrankfurterHttpClient): IExchangeRateProvider =
        FrankfurterExchangeRateProvider(client)
}
