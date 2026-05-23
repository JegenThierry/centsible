package beer.thierry.centsible.integrations.paypal

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@ConditionalOnProperty(value = ["integrations.paypal.enabled"], havingValue = "true", matchIfMissing = false)
class PaypalIntegrationConfig {

    private val log = LoggerFactory.getLogger(PaypalIntegrationConfig::class.java)

    @PostConstruct
    fun announce() {
        log.info("Provider PayPal enabled")
    }

    @Bean
    fun paypalHttpClient(): PaypalHttpClient = PaypalHttpClient(RestClient.builder().build())

    @Bean
    fun paypalProviderModule(client: PaypalHttpClient): PaypalProviderModule = PaypalProviderModule(client)
}
