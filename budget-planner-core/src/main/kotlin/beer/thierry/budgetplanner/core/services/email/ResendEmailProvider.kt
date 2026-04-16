package beer.thierry.budgetplanner.core.services.email

import beer.thierry.budgetplanner.api.services.email.IEmailProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class ResendEmailProvider(
    @Value("\${resend.api.key:dummy}") private val apiKey: String,
    @Value("\${resend.from.email:dummy@example.com}") private val fromEmail: String
) : IEmailProvider {
    private val restTemplate = RestTemplate()
    private val apiUrl = "https://api.resend.com/emails"

    override fun sendEmail(to: String, subject: String, content: String) {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBearerAuth(apiKey)

        val body = mapOf(
            "from" to fromEmail,
            "to" to arrayOf(to),
            "subject" to subject,
            "html" to content
        )

        val request = HttpEntity(body, headers)
        restTemplate.postForLocation(apiUrl, request)
    }
}
