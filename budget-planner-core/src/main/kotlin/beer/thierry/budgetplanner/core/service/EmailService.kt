package beer.thierry.budgetplanner.core.service

import beer.thierry.budgetplanner.api.service.email.IEmailProvider
import beer.thierry.budgetplanner.api.service.email.IEmailService
import beer.thierry.budgetplanner.api.model.user.User
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val emailProvider: IEmailProvider
) : IEmailService {
    override fun sendEmail(user: User, subject: String, content: String) {
        emailProvider.sendEmail(user.email, subject, content)
    }
}
