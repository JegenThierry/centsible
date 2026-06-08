package beer.thierry.centsible.integrations.paypal

import beer.thierry.centsible.integrations.support.HttpTimeoutProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@ConditionalOnProperty(value = ["integrations.paypal.enabled"], havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(HttpTimeoutProperties::class)
class PaypalIntegrationConfig {

    private val log = LoggerFactory.getLogger(PaypalIntegrationConfig::class.java)

    @PostConstruct
    fun announce() {
        log.info("Provider PayPal enabled")
    }

    @Bean
    fun paypalHttpClient(httpTimeouts: HttpTimeoutProperties): PaypalHttpClient = PaypalHttpClient(
        RestClient.builder()
            .requestFactory(httpTimeouts.requestFactory())
            .build()
    )

    @Bean
    fun paypalProviderModule(client: PaypalHttpClient): PaypalProviderModule = PaypalProviderModule(client)
}
