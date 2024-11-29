package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_GATEWAY
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.InvalidInputException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.InvalidRowException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.NotFoundException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class HmppsAlertsApiExceptionHandler {
  @ExceptionHandler(AlreadyExistsException::class)
  fun handleAlreadyExistsException(e: AlreadyExistsException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(
        ErrorResponse(
          status = HttpStatus.CONFLICT.value(),
          userMessage = "Duplicate failure: ${e.message}",
          developerMessage = "${e.resource} already exists with identifier ${e.identifier}",
        ),
      )

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.FORBIDDEN)
    .body(
      ErrorResponse(
        status = HttpStatus.FORBIDDEN.value(),
        userMessage = "Authentication problem. Check token and roles - ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
    val type = e.requiredType
    val message = if (type.isEnum) {
      "Parameter ${e.name} must be one of the following ${StringUtils.join(type.enumConstants, ", ")}"
    } else {
      "Parameter ${e.name} must be of type ${type.typeName}"
    }

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: $message",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: Couldn't read request body",
          developerMessage = e.message,
        ),
      )

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure(s): ${
          e.allErrors.map { it.defaultMessage }.distinct().sorted().joinToString(System.lineSeparator())
        }",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class, ValidationException::class)
  fun handleIllegalArgumentOrStateException(e: RuntimeException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.devMessage(),
      ),
    )

  private fun RuntimeException.devMessage(): String = when (this) {
    is InvalidInputException -> "Details => $name:$value"
    else -> message ?: "${this::class.simpleName}: ${cause?.message ?: ""}"
  }

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> =
    e.allErrors.map { it.defaultMessage }.distinct().sorted().let {
      val validationFailure = "Validation failure"
      val message = if (it.size > 1) {
        """
              |${validationFailure}s: 
              |${it.joinToString(System.lineSeparator())}
              |
        """.trimMargin()
      } else {
        "$validationFailure: ${it.joinToString(System.lineSeparator())}"
      }
      ResponseEntity
        .status(BAD_REQUEST)
        .body(
          ErrorResponse(
            status = BAD_REQUEST,
            userMessage = message,
            developerMessage = "400 BAD_REQUEST $message",
          ),
        )
    }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(NotFoundException::class)
  fun handleNotFoundException(e: NotFoundException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(404)
      .body(
        ErrorResponse(
          status = NOT_FOUND.value(),
          userMessage = "Not found: ${e.message}",
          developerMessage = "${e.resource} not found with identifier ${e.identifier}",
        ),
      )

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(METHOD_NOT_ALLOWED)
      .body(
        ErrorResponse(
          status = METHOD_NOT_ALLOWED,
          userMessage = "Method not allowed failure: ${e.message}",
          developerMessage = e.message,
        ),
      )

  @ExceptionHandler(DownstreamServiceException::class)
  fun handleDownstreamException(e: DownstreamServiceException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_GATEWAY)
    .body(
      ErrorResponse(
        status = BAD_GATEWAY,
        userMessage = "Downstream service exception: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )

  @ExceptionHandler(InvalidRowException::class)
  fun handleInvalidRow(ire: InvalidRowException): ResponseEntity<ErrorResponse> {
    val (rows, were) = if (ire.rows.size == 1) "row" to "was" else "rows" to "were"
    val rowNumbers = ire.rows.joinToString(", ")
    return ResponseEntity.status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Prison number from $rows $rowNumbers $were invalid",
        ),
      )
  }
}

class DownstreamServiceException(message: String, cause: Throwable) : RuntimeException(message, cause)
