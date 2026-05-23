package beer.thierry.centsible.integrations.banking.gocardless

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@ConditionalOnProperty(value = ["integrations.banking-gocardless.enabled"], havingValue = "true", matchIfMissing = false)
class GoCardlessIntegrationConfig(
    @Value("\${integrations.banking-gocardless.secret-id:}") private val secretId: String,
    @Value("\${integrations.banking-gocardless.secret-key:}") private val secretKey: String,
    @Value("\${integrations.banking-gocardless.api-base:https://bankaccountdata.gocardless.com/api/v2}") private val apiBase: String,
    @Value("\${integrations.banking-gocardless.min-sync-interval-seconds:21600}") private val minSyncIntervalSeconds: Long,
) {

    @PostConstruct
    fun validate() {
        if (secretId.isBlank() || secretKey.isBlank()) {
            throw IllegalStateException(
                "INTEGRATIONS_BANKING_GOCARDLESS_ENABLED=true but secret-id/secret-key not set"
            )
        }
    }

    @Bean
    fun goCardlessHttpClient(): GoCardlessHttpClient = GoCardlessHttpClient(
        restClient = RestClient.builder().baseUrl(apiBase).build(),
        secretId = secretId,
        secretKey = secretKey,
    )

    @Bean
    fun goCardlessProviderModule(client: GoCardlessHttpClient): GoCardlessProviderModule =
        GoCardlessProviderModule(client, minSyncIntervalSeconds)
}
