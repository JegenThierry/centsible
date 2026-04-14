package beer.thierry.budgetplannerrest.controller

import beer.thierry.budgetplannerrest.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            message = ex.message,
            details = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            message = ex.message,
            details = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            message = "An unexpected error occurred",
            details = ex.message ?: request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}
