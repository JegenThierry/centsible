package beer.thierry.centsible.core.services.email

import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.services.email.IEmailService
import beer.thierry.centsible.api.services.email.IPasswordResetEmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
import java.util.Locale

@Service
class PasswordResetEmailService(
    private val emailService: IEmailService,
    private val templateRenderer: EmailTemplateRenderer,
    // Must be the user-facing origin (where the UI is served), not the REST host.
    @Value("\${app.base-url:http://localhost:3000}") private val baseUrl: String,
) : IPasswordResetEmailService {

    private val log = LoggerFactory.getLogger(PasswordResetEmailService::class.java)

    override fun sendPasswordResetEmail(user: User, token: String, locale: Locale) {
        val url = HtmlUtils.htmlEscape("$baseUrl/auth/reset-password?token=$token")
        val greetingName = templateRenderer.renderGreetingName(
            user.firstName, "email.passwordReset.greeting.fallbackName", locale,
        )
        val copy = templateRenderer.buildCopy("email.passwordReset", locale, greetingName)
        try {
            emailService.sendEmail(user, copy.subject, templateRenderer.renderHtml(copy, url))
            log.info("Issued password reset email userId={} locale={}", user.id, locale.toLanguageTag())
        } catch (e: Exception) {
            log.error("Failed to issue password reset email userId={}", user.id, e)
            throw e
        }
    }
}
