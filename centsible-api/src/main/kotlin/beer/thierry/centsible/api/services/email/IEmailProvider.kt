package beer.thierry.centsible.api.services.email

interface IEmailProvider {
    fun sendEmail(to: String, subject: String, content: String)
}
