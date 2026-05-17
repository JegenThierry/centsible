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

@ControllerAdvice
class GlobalExceptionHandler(private val messageSource: MessageSource) {

    private fun t(key: String, vararg args: Any): String =
        messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale()) ?: key

    @ExceptionHandler(LocalizedException::class)
    fun handleLocalized(ex: LocalizedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val message = messageSource.getMessage(ex, LocaleContextHolder.getLocale())
        val error = ErrorResponse(message = message, details = request.getDescription(false))
        return ResponseEntity.status(ex.httpStatus).body(error)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: t("validation.generic.invalid"))
        }
        val error = ErrorResponse(
            message = t("error.validation.failed"),
            details = request.getDescription(false),
            fieldErrors = fieldErrors
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.constraintViolations.associate { violation ->
            val path = violation.propertyPath.toString()
            val field = path.substringAfterLast('.', path)
            field to violation.message
        }
        val error = ErrorResponse(
            message = t("error.validation.failed"),
            details = request.getDescription(false),
            fieldErrors = fieldErrors
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(
        ex: HttpMessageNotReadableException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val cause = ex.mostSpecificCause.message?.lineSequence()?.firstOrNull().orEmpty()
        val error = ErrorResponse(
            message = t("error.request.malformed"),
            details = cause.ifBlank { request.getDescription(false) }
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> {
        // Internal services may still throw IllegalArgumentException directly; keep the message
        // English. Prefer LocalizedException for anything user-facing.
        val error = ErrorResponse(
            message = ex.message ?: t("error.request.invalid"),
            details = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            message = ex.message ?: t("error.unexpected"),
            details = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            message = t("error.unexpected"),
            details = ex.message ?: request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}
