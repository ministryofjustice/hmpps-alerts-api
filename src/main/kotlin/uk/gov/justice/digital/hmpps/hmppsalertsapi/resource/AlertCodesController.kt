package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertCodeService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/alert-codes", produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertCodesController(
  private val alertCodeService: AlertCodeService,
) {

  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('$ROLE_ALERTS_ADMIN')")
  @PostMapping
  @Operation(
    summary = "Create an alert code",
    description = "Create a new alert code, typically from the Alerts UI",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Alert code created",
        content = [Content(schema = Schema(implementation = AlertCode::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found, the parent alert type has not been found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Conflict, the alert code already exists",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @UsernameHeader
  fun createAlertCode(
    @Valid @RequestBody createAlertCodeRequest: CreateAlertCodeRequest,
    httpRequest: HttpServletRequest,
  ): AlertCode = alertCodeService.createAlertCode(createAlertCodeRequest, httpRequest.alertRequestContext())

  private fun HttpServletRequest.alertRequestContext() =
    getAttribute(AlertRequestContext::class.simpleName) as AlertRequestContext
}
