package beer.thierry.centsiblerest.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

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
