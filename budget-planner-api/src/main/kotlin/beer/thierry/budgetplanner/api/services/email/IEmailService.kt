package beer.thierry.budgetplanner.api.services.email

import beer.thierry.budgetplanner.api.model.user.User

interface IEmailService {
    fun sendEmail(user: User, subject: String, content: String)
}
