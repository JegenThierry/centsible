package beer.thierry.centsiblerest.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(
    @Value("\${cors.allowed-origins:http://localhost:3000}") private val allowedOrigins: String,
) {

    private val log = LoggerFactory.getLogger(CorsConfig::class.java)

    @PostConstruct
    fun logAllowedOrigins() {
        val origins = parseOrigins()
        if (origins.isEmpty()) {
            log.warn("CORS allowed-origins list is empty; all cross-origin requests will be rejected.")
        } else {
            log.info("CORS allowed origins: {}", origins)
        }
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val origins = parseOrigins()

        val configuration = CorsConfiguration()
        configuration.allowedOrigins = origins
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
        configuration.exposedHeaders = listOf("Content-Disposition")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    private fun parseOrigins(): List<String> = allowedOrigins.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
