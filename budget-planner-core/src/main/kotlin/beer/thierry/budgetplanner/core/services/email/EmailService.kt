package beer.thierry.budgetplanner.core.services.email

import beer.thierry.budgetplanner.api.model.user.User
import beer.thierry.budgetplanner.api.services.email.IEmailProvider
import beer.thierry.budgetplanner.api.services.email.IEmailService
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val emailProvider: IEmailProvider
) : IEmailService {
    override fun sendEmail(user: User, subject: String, content: String) {
        emailProvider.sendEmail(user.email, subject, content)
    }
}
