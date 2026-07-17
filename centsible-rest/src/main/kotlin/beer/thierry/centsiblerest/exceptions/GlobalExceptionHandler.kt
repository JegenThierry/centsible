package beer.thierry.centsiblerest.exceptions

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.ErrorResponse
import beer.thierry.centsiblerest.logging.MDC_REQUEST_ID
import com.fasterxml.jackson.core.JacksonException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.util.unit.DataSize
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Extends [ResponseEntityExceptionHandler] so Spring MVC's own exceptions (missing parameter or
 * multipart part, unsupported method/media type, unknown URL, ...) keep their proper 4xx status
 * instead of falling into the catch-all below as 500s. Its hooks are overridden rather than left to
 * the defaults, which would answer with an RFC-7807 ProblemDetail instead of our [ErrorResponse].
 */
@ControllerAdvice
class GlobalExceptionHandler(
    private val messageSource: MessageSource,
    @param:Value("\${spring.servlet.multipart.max-file-size:10MB}") private val maxFileSize: DataSize,
) : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private fun t(key: String, vararg args: Any): String =
        messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale()) ?: key

    private fun body(
        message: String,
        request: WebRequest,
        details: String? = null,
        fieldErrors: Map<String, String>? = null,
    ): ErrorResponse = ErrorResponse(
        message = message,
        details = details ?: request.getDescription(false),
        fieldErrors = fieldErrors,
    )

    private fun error(
        status: HttpStatus,
        message: String,
        request: WebRequest,
        details: String? = null,
        fieldErrors: Map<String, String>? = null,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(body(message, request, details, fieldErrors))

    /**
     * Same payload as [error], typed for the framework hooks' `ResponseEntity<Any>` signature and
     * carrying the headers they set alongside the status (`Allow` on a 405, `Accept` on a 415).
     */
    private fun frameworkError(
        status: HttpStatusCode,
        message: String,
        request: WebRequest,
        headers: HttpHeaders = HttpHeaders.EMPTY,
        details: String? = null,
        fieldErrors: Map<String, String>? = null,
    ): ResponseEntity<Any> =
        ResponseEntity.status(status).headers(headers).body(body(message, request, details, fieldErrors))

    /**
     * Response for unexpected server errors. The raw exception is kept in the logs only; the client
     * gets the generic localized message plus the request's opaque correlation id (also returned in
     * the X-Request-Id header) so an operator can find the matching log line. Never echoes ex.message,
     * which routinely carries SQL/schema/constraint names or filesystem paths.
     */
    private fun serverError(request: WebRequest): ResponseEntity<ErrorResponse> =
        error(HttpStatus.INTERNAL_SERVER_ERROR, t("error.unexpected"), request, details = MDC.get(MDC_REQUEST_ID))

    @ExceptionHandler(LocalizedException::class)
    fun handleLocalized(ex: LocalizedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.valueOf(ex.httpStatus)
        if (status.is5xxServerError) {
            log.error("LocalizedException status={} key={}", ex.httpStatus, ex.messageKey, ex)
        } else {
            log.warn("LocalizedException status={} key={}", ex.httpStatus, ex.messageKey)
        }
        return error(status, messageSource.getMessage(ex, LocaleContextHolder.getLocale()), request)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: t("validation.generic.invalid"))
        }
        log.warn("Validation failed fields={}", fieldErrors.keys)
        return frameworkError(
            HttpStatus.BAD_REQUEST, t("error.validation.failed"), request, headers, fieldErrors = fieldErrors,
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.constraintViolations.associate { violation ->
            val path = violation.propertyPath.toString()
            path.substringAfterLast('.', path) to violation.message
        }
        log.warn("Constraint violation fields={}", fieldErrors.keys)
        return error(HttpStatus.BAD_REQUEST, t("error.validation.failed"), request, fieldErrors = fieldErrors)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val cause = ex.mostSpecificCause.message?.lineSequence()?.firstOrNull().orEmpty()
        log.warn("Malformed request body: {}", cause.ifBlank { ex.message })
        // The parser cause stays in the logs only — it can echo request payload fragments and
        // internal class names (ADR-0004). The correlation id lets an operator find it.
        return frameworkError(
            HttpStatus.BAD_REQUEST, t("error.request.malformed"), request, headers, MDC.get(MDC_REQUEST_ID),
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        // The converter cause names the internal enum/class it failed to build ("No enum constant
        // beer.thierry...CategoryType.BOGUS") and is untranslated, so it stays log-only (ADR-0004).
        val fieldErrors = mapOf(ex.name to t("validation.generic.invalid"))
        log.warn("Type mismatch on parameter '{}': {}", ex.name, ex.mostSpecificCause.message)
        return error(HttpStatus.BAD_REQUEST, t("error.validation.failed"), request, fieldErrors = fieldErrors)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException, request: WebRequest): ResponseEntity<ErrorResponse> {
        log.warn("Data integrity violation: {}", ex.mostSpecificCause.message)
        return error(HttpStatus.CONFLICT, t("error.conflict"), request)
    }

    @ExceptionHandler(JacksonException::class)
    fun handleJacksonParse(ex: JacksonException, request: WebRequest): ResponseEntity<ErrorResponse> {
        log.warn("Malformed JSON parameter: {}", ex.message)
        return error(HttpStatus.BAD_REQUEST, t("error.request.malformed"), request)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> {
        log.warn("IllegalArgumentException: {}", ex.message)
        // `require(...)` messages are developer-facing English precondition text, not localized
        // user copy (ADR-0004) — the client gets the generic key, the log keeps the specifics.
        return error(HttpStatus.BAD_REQUEST, t("error.request.invalid"), request, details = MDC.get(MDC_REQUEST_ID))
    }

    override fun handleMaxUploadSizeExceededException(
        ex: MaxUploadSizeExceededException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        // Servlet-level multipart rejection happens before any resource code runs; without this
        // handler it falls through to the generic 500 instead of a clean 413.
        log.warn("Upload rejected: exceeds spring.servlet.multipart.max-file-size ({})", maxFileSize)
        return frameworkError(
            HttpStatus.PAYLOAD_TOO_LARGE, t("error.upload.tooLarge", maxFileSize.toMegabytes()), request, headers,
        )
    }

    override fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        // Every unknown URL raises this since Boot 3.2 — scanners alone would fill the log with
        // stack traces at error level.
        log.debug("No handler for {} {}", ex.httpMethod, ex.resourcePath)
        return frameworkError(status, t("error.request.notFound"), request, headers)
    }

    override fun handleAsyncRequestNotUsableException(
        ex: AsyncRequestNotUsableException,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        // The client is already gone and the response is unusable; there is nothing to send back.
        log.debug("Async request no longer usable: {}", ex.message)
        return null
    }

    /**
     * Terminal hook for the framework exceptions without a dedicated override above (unsupported
     * method → 405, media type → 415/406, missing parameter/part → 400, ...). The status Spring
     * derived is kept; only the payload is swapped for ours.
     */
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (statusCode.is5xxServerError) {
            log.error("Framework exception status={}", statusCode.value(), ex)
            return frameworkError(statusCode, t("error.unexpected"), request, headers, MDC.get(MDC_REQUEST_ID))
        }
        log.warn("Request rejected status={} type={}: {}", statusCode.value(), ex.javaClass.simpleName, ex.message)
        // ex.message names the required parameter/part or the offending media type — useful in the
        // log, but it is untranslated developer text, so the client gets the generic key (ADR-0004).
        return frameworkError(statusCode, t("error.request.invalid"), request, headers, MDC.get(MDC_REQUEST_ID))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException, request: WebRequest): ResponseEntity<ErrorResponse> {
        log.error("IllegalStateException", ex)
        return serverError(request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", ex)
        return serverError(request)
    }
}
