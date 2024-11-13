package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlertPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkCreateAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.BulkAlertService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/bulk-alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class BulkAlertsController(
  private val bulkAlertService: BulkAlertService,
) {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Create alerts for multiple people in bulk",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Alerts created successfully",
        content = [Content(schema = Schema(implementation = BulkAlert::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
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
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @UsernameHeader
  @SourceHeader
  fun bulkCreateAlerts(
    @Valid
    @RequestBody
    @Parameter(
      description = "The data for bulk creating alerts for multiple people",
      required = true,
    )
    request: BulkCreateAlerts,
    httpRequest: HttpServletRequest,
  ): BulkAlert = bulkAlertService.bulkCreateAlerts(request, httpRequest.alertRequestContext())

  @ResponseStatus(HttpStatus.OK)
  @PostMapping("/plan")
  @Operation(
    summary = "Plan the creation of alerts for multiple people in bulk",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alerts creation plan generated successfully",
        content = [Content(schema = Schema(implementation = BulkAlert::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
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
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @UsernameHeader
  @SourceHeader
  fun planBulkCreateAlerts(
    @Valid
    @RequestBody
    @Parameter(
      description = "The data for bulk creating alerts for multiple people",
      required = true,
    )
    request: BulkCreateAlerts,
    httpRequest: HttpServletRequest,
  ): BulkAlertPlan = bulkAlertService.planBulkCreateAlerts(request, httpRequest.alertRequestContext())

  private fun HttpServletRequest.alertRequestContext() =
    getAttribute(AlertRequestContext::class.simpleName) as AlertRequestContext
}
