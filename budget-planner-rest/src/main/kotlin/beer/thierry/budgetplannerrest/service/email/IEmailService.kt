package beer.thierry.budgetplannerrest.service.email

import beer.thierry.budgetplannerrest.model.user.User

interface IEmailService {
    fun sendEmail(user: User, subject: String, content: String)
}
