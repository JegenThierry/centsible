package beer.thierry.centsiblerest.exceptions

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.ErrorResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
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

    @ExceptionHandler(LocalizedException::class)
    fun handleLocalized(ex: LocalizedException, request: WebRequest): ResponseEntity<ErrorResponse> =
        error(HttpStatus.valueOf(ex.httpStatus), messageSource.getMessage(ex, LocaleContextHolder.getLocale()), request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: t("validation.generic.invalid"))
        }
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
        return error(HttpStatus.BAD_REQUEST, t("error.validation.failed"), request, fieldErrors = fieldErrors)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(ex: HttpMessageNotReadableException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val cause = ex.mostSpecificCause.message?.lineSequence()?.firstOrNull().orEmpty()
        return error(HttpStatus.BAD_REQUEST, t("error.request.malformed"), request, details = cause.ifBlank { null })
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = mapOf(ex.name to (ex.mostSpecificCause.message ?: t("validation.generic.invalid")))
        return error(HttpStatus.BAD_REQUEST, t("error.validation.failed"), request, fieldErrors = fieldErrors)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> =
        // Internal services may still throw IllegalArgumentException directly; keep the message
        // English. Prefer LocalizedException for anything user-facing.
        error(HttpStatus.BAD_REQUEST, ex.message ?: t("error.request.invalid"), request)

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException, request: WebRequest): ResponseEntity<ErrorResponse> =
        error(HttpStatus.INTERNAL_SERVER_ERROR, ex.message ?: t("error.unexpected"), request)

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> =
        error(HttpStatus.INTERNAL_SERVER_ERROR, t("error.unexpected"), request, details = ex.message)
}
