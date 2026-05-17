package beer.thierry.centsible.integrations.manual

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(value = ["integrations.manual.enabled"], havingValue = "true", matchIfMissing = true)
class ManualIntegrationConfig {

    @Bean
    fun manualProviderModule(): ManualProviderModule = ManualProviderModule()
}
