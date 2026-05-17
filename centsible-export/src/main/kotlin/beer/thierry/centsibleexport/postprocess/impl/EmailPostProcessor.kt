package beer.thierry.centsibleexport.postprocess.impl

import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.PostProcessingType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsibleexport.postprocess.PostProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.HtmlUtils
import java.util.Base64

@Component
class EmailPostProcessor(
    private val data: IExportDataRepository,
    @param:Value("\${resend.api.key:dummy}") private val apiKey: String,
    @param:Value("\${resend.from.email:onboarding@resend.dev}") private val fromEmail: String,
) : PostProcessor {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()
    private val apiUrl = "https://api.resend.com/emails"

    override fun supports(): PostProcessingType = PostProcessingType.SEND_EMAIL

    override fun execute(job: ExportJobDTO, pdf: ByteArray, pdfFilename: String, config: Map<String, Any?>) {
        val recipient = (config["recipient"] as? String)?.takeIf { it.isNotBlank() }
            ?: data.fetchUserById(job.userId)?.email
            ?: error("No recipient email resolved for job ${job.id}")

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(apiKey)
        }
        val body = mapOf(
            "from" to fromEmail,
            "to" to listOf(recipient),
            "subject" to "Your export: ${job.title}",
            "html" to """
                <p>Hello,</p>
                <p>Your export <strong>${HtmlUtils.htmlEscape(job.title)}</strong> is attached.</p>
                <p>— Centsible</p>
            """.trimIndent(),
            "attachments" to listOf(
                mapOf(
                    "filename" to pdfFilename,
                    "content" to Base64.getEncoder().encodeToString(pdf),
                )
            ),
        )

        val request = HttpEntity(body, headers)
        restTemplate.postForLocation(apiUrl, request)
        log.info("Sent export {} to {}", job.id, recipient)
    }
}
