package beer.thierry.budgetplannerrest.service.email

interface IEmailProvider {
    fun sendEmail(to: String, subject: String, content: String)
}
