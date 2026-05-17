package beer.thierry.centsible.api.services.email

import beer.thierry.centsible.api.model.user.User

interface IEmailService {
    fun sendEmail(user: User, subject: String, content: String)
}
