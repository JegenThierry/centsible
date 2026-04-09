package beer.thierry.budgetplannerrest.service.email

import beer.thierry.budgetplannerrest.model.user.User
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val emailProvider: IEmailProvider
) : IEmailService {
    override fun sendEmail(user: User, subject: String, content: String) {
        emailProvider.sendEmail(user.email, subject, content)
    }
}
