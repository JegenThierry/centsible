package beer.thierry.centsible.integrations.fx.frankfurter

import beer.thierry.centsible.api.services.integrations.IExchangeRateProvider
import beer.thierry.centsible.integrations.support.HttpTimeoutProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@ConditionalOnProperty(value = ["fx.frankfurter.enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(HttpTimeoutProperties::class)
class FrankfurterFxConfig(
    @param:Value("\${fx.frankfurter.api-base:https://api.frankfurter.dev}") private val apiBase: String,
) {

    private val log = LoggerFactory.getLogger(FrankfurterFxConfig::class.java)

    @PostConstruct
    fun announce() {
        log.info("FX rate provider Frankfurter enabled (apiBase={})", apiBase)
    }

    @Bean
    fun frankfurterHttpClient(httpTimeouts: HttpTimeoutProperties): FrankfurterHttpClient = FrankfurterHttpClient(
        RestClient.builder()
            .requestFactory(httpTimeouts.requestFactory())
            .build(),
        apiBase,
    )

    @Bean
    fun frankfurterExchangeRateProvider(client: FrankfurterHttpClient): IExchangeRateProvider =
        FrankfurterExchangeRateProvider(client)
}
