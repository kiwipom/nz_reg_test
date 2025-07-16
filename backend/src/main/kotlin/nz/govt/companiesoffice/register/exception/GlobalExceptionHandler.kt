package nz.govt.companiesoffice.register.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Resource Not Found",
            message = ex.message ?: "Resource not found",
            path = ex.resourceType,
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(ex: ValidationException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Error",
            message = ex.message ?: "Validation failed",
            path = ex.field,
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.CONFLICT.value(),
            error = "Business Rule Violation",
            message = ex.message ?: "Business rule violation",
            path = ex.context,
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
            path = null,
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}

data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String?,
)

class ResourceNotFoundException(val resourceType: String, message: String) : RuntimeException(message)
class ValidationException(val field: String, message: String) : RuntimeException(message)
class BusinessException(val context: String, message: String) : RuntimeException(message)
