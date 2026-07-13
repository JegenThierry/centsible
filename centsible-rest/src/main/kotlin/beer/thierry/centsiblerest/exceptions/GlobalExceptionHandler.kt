package beer.thierry.centsiblerest.exceptions

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.ErrorResponse
import beer.thierry.centsiblerest.logging.MDC_REQUEST_ID
import com.fasterxml.jackson.core.JacksonException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@ControllerAdvice
class GlobalExceptionHandler(private val messageSource: MessageSource) {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private fun t(key: String, vararg args: Any): String =
        messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale()) ?: key

    private fun error(
        status: HttpStatus,
        message: String,
        request: WebRequest,
        details: String? = null,
        fieldErrors: Map<String, String>? = null,
    ): ResponseEntity<ErrorResponse> = ResponseEntity.status(status).body(
        ErrorResponse(
            message = message,
            details = details ?: request.getDescription(false),
            fieldErrors = fieldErrors,
        )
    )

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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: t("validation.generic.invalid"))
        }
        log.warn("Validation failed fields={}", fieldErrors.keys)
        return error(HttpStatus.BAD_REQUEST, t("error.validation.failed"), request, fieldErrors = fieldErrors)
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

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(ex: HttpMessageNotReadableException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val cause = ex.mostSpecificCause.message?.lineSequence()?.firstOrNull().orEmpty()
        log.warn("Malformed request body: {}", cause.ifBlank { ex.message })
        // The parser cause stays in the logs only — it can echo request payload fragments and
        // internal class names (ADR-0004). The correlation id lets an operator find it.
        return error(HttpStatus.BAD_REQUEST, t("error.request.malformed"), request, details = MDC.get(MDC_REQUEST_ID))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = mapOf(ex.name to (ex.mostSpecificCause.message ?: t("validation.generic.invalid")))
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
