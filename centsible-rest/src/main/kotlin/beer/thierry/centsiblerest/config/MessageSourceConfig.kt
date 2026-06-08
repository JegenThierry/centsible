package beer.thierry.centsiblerest.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

/**
 * App-wide internationalisation: validation messages, exception responses, and email copy
 * all resolve through the same message bundle (`messages_{en,fr,de}.properties` on the
 * classpath). The active locale comes from the `Accept-Language` header that the UI attaches
 * to every request via its axios plugin; falling back to English keeps API testing clients
 * (Bruno, curl) functional without ceremony.
 */
@Configuration
class MessageSourceConfig {

    private val supportedLocales = listOf(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN)

    @Bean
    fun messageSource(): MessageSource {
        val source = ReloadableResourceBundleMessageSource()
        source.setBasenames("classpath:messages", "classpath:email/messages")
        source.setDefaultEncoding("UTF-8")
        source.setFallbackToSystemLocale(false)
        source.setUseCodeAsDefaultMessage(true)
        source.setAlwaysUseMessageFormat(true)
        return source
    }

    /** Routes Hibernate Validator's `{key}` placeholders through our [messageSource]. */
    @Bean
    fun validator(messageSource: MessageSource): LocalValidatorFactoryBean =
        LocalValidatorFactoryBean().apply { setValidationMessageSource(messageSource) }

    @Bean
    fun localeResolver(): LocaleResolver = AcceptHeaderLocaleResolver().apply {
        setDefaultLocale(Locale.ENGLISH)
        setSupportedLocales(supportedLocales)
    }
}
