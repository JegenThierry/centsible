package beer.thierry.centsible.core.services.email

import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.services.email.IEmailProvider
import beer.thierry.centsible.api.services.email.IEmailService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val emailProvider: IEmailProvider
) : IEmailService {

    private val log = LoggerFactory.getLogger(EmailService::class.java)

    override fun sendEmail(user: User, subject: String, content: String) {
        try {
            emailProvider.sendEmail(user.email, subject, content)
            log.info("Sent email userId={} subject='{}'", user.id, subject)
        } catch (e: Exception) {
            log.error("Failed to send email userId={} subject='{}'", user.id, subject, e)
            throw e
        }
    }
}
