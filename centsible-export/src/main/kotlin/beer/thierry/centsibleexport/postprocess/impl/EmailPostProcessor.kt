package beer.thierry.centsibleexport.postprocess.impl

import beer.thierry.centsible.api.model.export.ExportJobDTO
import beer.thierry.centsible.api.model.export.PostProcessingType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsibleexport.postprocess.PostProcessor
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.HtmlUtils
import java.time.Duration
import java.util.Base64

@Component
class EmailPostProcessor(
    private val data: IExportDataRepository,
    @param:Value("\${resend.api.key:dummy}") private val apiKey: String,
    @param:Value("\${resend.from.email:onboarding@resend.dev}") private val fromEmail: String,
    @Value("\${resend.connect-timeout-ms:10000}") connectTimeoutMs: Long,
    @Value("\${resend.read-timeout-ms:30000}") readTimeoutMs: Long,
) : PostProcessor {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restTemplate = RestTemplate(
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
            setReadTimeout(Duration.ofMillis(readTimeoutMs))
        }
    )

    private val emailRetry: Retry = Retry.of(
        "resend-email",
        RetryConfig.custom<Any>()
            .maxAttempts(MAX_ATTEMPTS)
            .intervalFunction(
                IntervalFunction.ofExponentialBackoff(
                    Duration.ofMillis(INITIAL_BACKOFF_MS),
                    BACKOFF_MULTIPLIER,
                    Duration.ofMillis(MAX_BACKOFF_MS),
                )
            )
            .retryOnException { isTransientFailure(it) }
            .build(),
    ).apply {
        eventPublisher.onRetry { event ->
            log.warn(
                "Retrying export email send attempt={} lastException={}",
                event.numberOfRetryAttempts, event.lastThrowable?.javaClass?.simpleName,
            )
        }
    }

    override fun supports(): PostProcessingType = PostProcessingType.SEND_EMAIL

    override fun execute(job: ExportJobDTO, pdf: ByteArray, pdfFilename: String, config: Map<String, Any?>) {
        log.info("Enqueuing export email jobId={} userId={} bytes={}", job.id, job.userId, pdf.size)
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

        try {
            val request = HttpEntity(body, headers)
            emailRetry.executeCallable { restTemplate.postForLocation(RESEND_API_URL, request) }
            log.info(
                "Sent export email jobId={} userId={} recipientDomain={}",
                job.id, job.userId, recipient.substringAfter('@', "unknown"),
            )
        } catch (ex: Exception) {
            log.error("Failed to send export email jobId={} userId={}", job.id, job.userId, ex)
            throw ex
        }
    }

    private fun isTransientFailure(e: Throwable): Boolean = when (e) {
        is ResourceAccessException -> true
        is HttpServerErrorException -> true
        is HttpClientErrorException -> e.statusCode.value() == 429
        else -> false
    }

    private companion object {
        const val RESEND_API_URL = "https://api.resend.com/emails"
        const val MAX_ATTEMPTS = 3
        const val INITIAL_BACKOFF_MS = 500L
        const val BACKOFF_MULTIPLIER = 2.0
        const val MAX_BACKOFF_MS = 5_000L
    }
}
