package beer.thierry.centsible.api.services.email

import beer.thierry.centsible.api.model.user.User

/** Domain-level email entry point that resolves a [User] to their address and delegates to an [IEmailProvider]. */
interface IEmailService {
    fun sendEmail(user: User, subject: String, content: String)
}
