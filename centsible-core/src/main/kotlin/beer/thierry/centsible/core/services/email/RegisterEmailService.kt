package beer.thierry.centsible.core.services.email

import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.services.email.IEmailService
import beer.thierry.centsible.api.services.email.IRegisterEmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
import java.util.Locale

@Service
class RegisterEmailService(
    private val emailService: IEmailService,
    private val templateRenderer: EmailTemplateRenderer,
    @Value("\${app.base-url:http://localhost:3000}") private val baseUrl: String,
) : IRegisterEmailService {

    private val log = LoggerFactory.getLogger(RegisterEmailService::class.java)

    override fun sendRegistrationEmail(user: User, token: String, locale: Locale) {
        val url = HtmlUtils.htmlEscape("$baseUrl/auth/confirm?token=$token")
        val greetingName = templateRenderer.renderGreetingName(
            user.firstName, "email.register.greeting.fallbackName", locale,
        )
        val copy = templateRenderer.buildCopy("email.register", locale, greetingName)
        try {
            emailService.sendEmail(user, copy.subject, templateRenderer.renderHtml(copy, url))
            log.info("Issued registration confirmation email userId={} locale={}", user.id, locale.toLanguageTag())
        } catch (e: Exception) {
            log.error("Failed to issue registration confirmation email userId={}", user.id, e)
            throw e
        }
    }
}
