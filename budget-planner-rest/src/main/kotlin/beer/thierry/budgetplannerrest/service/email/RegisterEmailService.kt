package beer.thierry.budgetplannerrest.service.email

import beer.thierry.budgetplannerrest.model.user.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RegisterEmailService(
    private val emailService: IEmailService,
    @Value("\${app.base-url}") private val baseUrl: String
) : IRegisterEmailService {
    override fun sendRegistrationEmail(user: User, token: String) {
        val confirmationUrl = "$baseUrl/api/auth/confirm?token=$token&username=${user.username}"
        val subject = "Confirm your account"
        val content = """
            <h1>Welcome to Budget Planner</h1>
            <p>Please confirm your account by clicking the button below:</p>
            <a href="$confirmationUrl" style="background-color: #4CAF50; color: white; padding: 16px 24px; text-decoration: none; display: inline-block; border-radius: 4px;">Confirm Account</a>
        """.trimIndent()

        emailService.sendEmail(user, subject, content)
    }
}
