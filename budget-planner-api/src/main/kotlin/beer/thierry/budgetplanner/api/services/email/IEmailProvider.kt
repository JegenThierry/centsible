package beer.thierry.budgetplanner.api.services.email

interface IEmailProvider {
    fun sendEmail(to: String, subject: String, content: String)
}
