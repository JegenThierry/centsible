package beer.thierry.budgetplanner.core.services.email

import beer.thierry.budgetplanner.api.services.email.IEmailProvider
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class ResendEmailProvider(
    @Value("\${resend.api.key:dummy}") private val apiKey: String,
    @Value("\${resend.from.email:dummy@example.com}") private val fromEmail: String
) : IEmailProvider {
    private val log = LoggerFactory.getLogger(ResendEmailProvider::class.java)
    private val restTemplate = RestTemplate()
    private val apiUrl = "https://api.resend.com/emails"

    @PostConstruct
    fun logConfig() {
        if (apiKey == "dummy" || apiKey.isBlank()) {
            log.warn("Resend API key not configured — set RESEND_API_KEY in .env. Email sending will fail.")
        }
        if (fromEmail == "dummy@example.com" || fromEmail.isBlank()) {
            log.warn("Resend from-address not configured — set RESEND_FROM_EMAIL in .env. Email sending will fail.")
        } else if (fromEmail.endsWith("@resend.dev")) {
            log.warn(
                "Resend from-address is {} — the Resend sandbox domain only allows sending to the email address " +
                    "registered on your Resend account. Verify your own domain at https://resend.com/domains to send to anyone.",
                fromEmail,
            )
        }
    }

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
        try {
            val response = restTemplate.postForEntity(apiUrl, request, String::class.java)
            log.info("Resend accepted email to {} (status {})", to, response.statusCode.value())
        } catch (e: HttpStatusCodeException) {
            log.error(
                "Resend rejected email to {} (status {}): {}",
                to,
                e.statusCode.value(),
                e.responseBodyAsString,
            )
            throw e
        } catch (e: RestClientException) {
            log.error("Resend call failed for {}: {}", to, e.message)
            throw e
        }
    }
}
