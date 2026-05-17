package beer.thierry.centsible.core.services.email

import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.services.email.IEmailService
import beer.thierry.centsible.api.services.email.IPasswordResetEmailService
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
    override fun sendPasswordResetEmail(user: User, token: String, locale: Locale) {
        val url = HtmlUtils.htmlEscape("$baseUrl/auth/reset-password?token=$token")
        val greetingName = templateRenderer.renderGreetingName(
            user.firstName, "email.passwordReset.greeting.fallbackName", locale,
        )
        val copy = templateRenderer.buildCopy("email.passwordReset", locale, greetingName)
        emailService.sendEmail(user, copy.subject, templateRenderer.renderHtml(copy, url))
    }
}
