package beer.thierry.budgetplanner.api.service.email

interface IEmailProvider {
    fun sendEmail(to: String, subject: String, content: String)
}
