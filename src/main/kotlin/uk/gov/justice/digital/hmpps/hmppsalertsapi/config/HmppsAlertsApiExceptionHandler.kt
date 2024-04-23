package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlertCodeNotFoundException
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.AlertTypeNotFound
import uk.gov.justice.digital.hmpps.hmppsalertsapi.exceptions.ExistingActiveAlertTypeWithCodeException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class HmppsAlertsApiExceptionHandler {
  @ExceptionHandler(AlertCodeNotFoundException::class)
  fun handleExistingActiveAlertTypeWithCodeException(e: AlertCodeNotFoundException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(404)
      .body(
        ErrorResponse(
          status = NOT_FOUND.value(),
          userMessage = "Not found: ${e.message}",
          developerMessage = e.message,
        ),
      )

  @ExceptionHandler(AlertTypeNotFound::class)
  fun handleExistingActiveAlertTypeWithCodeException(e: AlertTypeNotFound): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(404)
      .body(
        ErrorResponse(
          status = NOT_FOUND.value(),
          userMessage = "Not found: ${e.message}",
          developerMessage = e.message,
        ),
      )

  @ExceptionHandler(ExistingActiveAlertTypeWithCodeException::class)
  fun handleExistingActiveAlertTypeWithCodeException(e: ExistingActiveAlertTypeWithCodeException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(409)
      .body(
        ErrorResponse(
          status = HttpStatus.CONFLICT.value(),
          userMessage = "Duplicate failure: ${e.message}",
          developerMessage = e.message,
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
    ).also { log.info("Access denied exception: {}", e.message) }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Missing servlet request parameter exception: {}", e.message) }

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
      ).also { log.info("Method argument type mismatch exception: {}", e.message) }
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: Couldn't read request body",
        developerMessage = e.message,
      ),
    ).also { log.info("HTTP message not readable exception: {}", e.message) }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Illegal argument exception: {}", e.message) }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> =
    e.allErrors.map { it.toString() }.distinct().sorted().joinToString("\n").let { validationErrors ->
      ResponseEntity
        .status(BAD_REQUEST)
        .body(
          ErrorResponse(
            status = BAD_REQUEST,
            userMessage = "Validation failure(s): ${
              e.allErrors.map { it.defaultMessage }.distinct().sorted().joinToString("\n")
            }",
            developerMessage = "${e.message} $validationErrors",
          ),
        ).also { log.info("Validation exception: $validationErrors\n {}", e.message) }
    }

  @ExceptionHandler(AlertNotFoundException::class)
  fun handleAlertNotFoundException(e: AlertNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "Alert not found: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Alert not found exception: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(METHOD_NOT_ALLOWED)
    .body(
      ErrorResponse(
        status = METHOD_NOT_ALLOWED,
        userMessage = "Method not allowed failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Method not allowed exception: {}", e.message) }

  @ExceptionHandler(ExistingActiveAlertWithCodeException::class)
  fun handleExistingActiveAlertWithCodeException(e: ExistingActiveAlertWithCodeException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.CONFLICT)
    .body(
      ErrorResponse(
        status = HttpStatus.CONFLICT.value(),
        userMessage = "Duplicate failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Existing active alert with code exception: {}", e.message) }

  @ExceptionHandler(DownstreamServiceException::class)
  fun handleException(e: DownstreamServiceException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_GATEWAY)
    .body(
      ErrorResponse(
        status = BAD_GATEWAY,
        userMessage = "Downstream service exception: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.warn("Downstream service exception", e.cause) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

class DownstreamServiceException(message: String, cause: Throwable) : Exception(message, cause)
class AlertNotFoundException(message: String) : Exception(message)
class ExistingActiveAlertWithCodeException(prisonNumber: String, alertCode: String) : Exception("Active alert with code '$alertCode' already exists for prison number '$prisonNumber'")
