package beer.thierry.centsible.integrations.banking.gocardless

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
@ConditionalOnProperty(value = ["integrations.banking-gocardless.enabled"], havingValue = "true", matchIfMissing = false)
class GoCardlessIntegrationConfig(
    @Value("\${integrations.banking-gocardless.secret-id:}") private val secretId: String,
    @Value("\${integrations.banking-gocardless.secret-key:}") private val secretKey: String,
    @Value("\${integrations.banking-gocardless.api-base:https://bankaccountdata.gocardless.com/api/v2}") private val apiBase: String,
    @Value("\${integrations.banking-gocardless.min-sync-interval-seconds:21600}") private val minSyncIntervalSeconds: Long,
    @Value("\${integrations.http.connect-timeout-ms:10000}") private val connectTimeoutMs: Long,
    @Value("\${integrations.http.read-timeout-ms:30000}") private val readTimeoutMs: Long,
) {

    private val log = LoggerFactory.getLogger(GoCardlessIntegrationConfig::class.java)

    @PostConstruct
    fun validate() {
        if (secretId.isBlank() || secretKey.isBlank()) {
            val msg = "INTEGRATIONS_BANKING_GOCARDLESS_ENABLED=true but secret-id/secret-key not set"
            log.error(msg)
            throw IllegalStateException(msg)
        }
        log.info(
            "Provider GoCardless enabled apiBase={} minSyncIntervalSeconds={}",
            apiBase, minSyncIntervalSeconds,
        )
    }

    @Bean
    fun goCardlessHttpClient(): GoCardlessHttpClient = GoCardlessHttpClient(
        restClient = RestClient.builder()
            .baseUrl(apiBase)
            .requestFactory(timeoutRequestFactory(Duration.ofMillis(connectTimeoutMs), Duration.ofMillis(readTimeoutMs)))
            .build(),
        secretId = secretId,
        secretKey = secretKey,
    )

    @Bean
    fun goCardlessProviderModule(client: GoCardlessHttpClient): GoCardlessProviderModule =
        GoCardlessProviderModule(client, minSyncIntervalSeconds)
}
