package beer.thierry.budgetplanner.api.service.email

import beer.thierry.budgetplanner.api.model.user.User

interface IEmailService {
    fun sendEmail(user: User, subject: String, content: String)
}
