package beer.thierry.centsible.integrations.banking.gocardless

import beer.thierry.centsible.integrations.support.HttpTimeoutProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(value = ["integrations.banking-gocardless.enabled"], havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(HttpTimeoutProperties::class)
class GoCardlessIntegrationConfig(
    @Value("\${integrations.banking-gocardless.secret-id:}") private val secretId: String,
    @Value("\${integrations.banking-gocardless.secret-key:}") private val secretKey: String,
    @Value("\${integrations.banking-gocardless.api-base:https://bankaccountdata.gocardless.com/api/v2}") private val apiBase: String,
    @Value("\${integrations.banking-gocardless.min-sync-interval-seconds:21600}") private val minSyncIntervalSeconds: Long,
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
    fun goCardlessHttpClient(httpTimeouts: HttpTimeoutProperties): GoCardlessHttpClient = GoCardlessHttpClient(
        restClient = httpTimeouts.restClientBuilder(apiBase).build(),
        secretId = secretId,
        secretKey = secretKey,
    )

    @Bean
    fun goCardlessProviderModule(client: GoCardlessHttpClient): GoCardlessProviderModule =
        GoCardlessProviderModule(client, minSyncIntervalSeconds)
}
