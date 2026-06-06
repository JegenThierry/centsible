package beer.thierry.centsible.integrations.paypal

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
@ConditionalOnProperty(value = ["integrations.paypal.enabled"], havingValue = "true", matchIfMissing = false)
class PaypalIntegrationConfig(
    @param:Value("\${integrations.http.connect-timeout-ms:10000}") private val connectTimeoutMs: Long,
    @param:Value("\${integrations.http.read-timeout-ms:30000}") private val readTimeoutMs: Long,
) {

    private val log = LoggerFactory.getLogger(PaypalIntegrationConfig::class.java)

    @PostConstruct
    fun announce() {
        log.info("Provider PayPal enabled")
    }

    @Bean
    fun paypalHttpClient(): PaypalHttpClient = PaypalHttpClient(
        RestClient.builder()
            .requestFactory(timeoutRequestFactory(Duration.ofMillis(connectTimeoutMs), Duration.ofMillis(readTimeoutMs)))
            .build()
    )

    @Bean
    fun paypalProviderModule(client: PaypalHttpClient): PaypalProviderModule = PaypalProviderModule(client)
}
