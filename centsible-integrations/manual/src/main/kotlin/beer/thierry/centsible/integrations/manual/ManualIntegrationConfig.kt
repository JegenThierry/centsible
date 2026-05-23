package beer.thierry.centsible.integrations.manual

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(value = ["integrations.manual.enabled"], havingValue = "true", matchIfMissing = true)
class ManualIntegrationConfig {

    private val log = LoggerFactory.getLogger(ManualIntegrationConfig::class.java)

    @PostConstruct
    fun announce() {
        log.info("Provider Manual enabled (no external API)")
    }

    @Bean
    fun manualProviderModule(): ManualProviderModule = ManualProviderModule()
}
