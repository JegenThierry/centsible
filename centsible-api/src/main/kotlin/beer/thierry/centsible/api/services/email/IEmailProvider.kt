package beer.thierry.centsible.api.services.email

/** Low-level transport that delivers an email to a raw [to] address; implemented per email vendor. */
interface IEmailProvider {
    fun sendEmail(to: String, subject: String, content: String)
}
