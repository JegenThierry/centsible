package beer.thierry.centsible.integrations.paypal

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@ConditionalOnProperty(value = ["integrations.paypal.enabled"], havingValue = "true", matchIfMissing = false)
class PaypalIntegrationConfig {

    @Bean
    fun paypalHttpClient(): PaypalHttpClient = PaypalHttpClient(RestClient.builder().build())

    @Bean
    fun paypalProviderModule(client: PaypalHttpClient): PaypalProviderModule = PaypalProviderModule(client)
}
