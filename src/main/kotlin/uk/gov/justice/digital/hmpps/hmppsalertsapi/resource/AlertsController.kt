package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.NOMIS_SYNC_ONLY
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.RO_OPERATIONS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.RW_OPERATIONS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping("/alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertsController(private val alertService: AlertService) {

  @Tag(name = RO_OPERATIONS)
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/{alertUuid}")
  @Operation(
    summary = "Get an alert by its unique identifier",
    description = "Returns the alert with the matching identifier.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert found",
        content = [Content(schema = Schema(implementation = Alert::class))],
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
        description = "The alert associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RO', '$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  fun retrieveAlert(
    @PathVariable
    @Parameter(
      description = "Alert unique identifier",
      required = true,
    )
    alertUuid: UUID,
  ): Alert = alertService.retrieveAlert(alertUuid)

  @Tag(name = RW_OPERATIONS)
  @ResponseStatus(HttpStatus.OK)
  @PutMapping("/{alertUuid}")
  @Operation(
    summary = "Update an alert",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert updated successfully",
        content = [Content(schema = Schema(implementation = Alert::class))],
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
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @UsernameHeader
  @SourceHeader
  fun updateAlert(
    @PathVariable
    @Parameter(
      description = "Alert unique identifier",
      required = true,
    )
    alertUuid: UUID,
    @Valid
    @RequestBody
    @Parameter(
      description = "The alert data to use to update an alert in the service",
      required = true,
    )
    request: UpdateAlert,
    httpRequest: HttpServletRequest,
  ): Alert = alertService.updateAlert(alertUuid, request, httpRequest.alertRequestContext())

  @Tag(name = NOMIS_SYNC_ONLY)
  @DeleteMapping("/{alertUuid}")
  @ResponseStatus(NO_CONTENT)
  @Operation(
    summary = "Delete an alert",
    description = "This endpoint fully removes the alert from the system. It is used when an alert " +
      "has been created in error or should otherwise be removed from this service. " +
      "This endpoint will return 200 OK if the alert was not found or already deleted. This is to prevent low value warnings being logged.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Alert deleted",
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
        description = "Alert was not found or already deleted",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @UsernameHeader
  @SourceHeader
  fun deleteAlert(
    @PathVariable
    @Parameter(
      description = "Alert unique identifier",
      required = true,
    )
    alertUuid: UUID,
    httpRequest: HttpServletRequest,
  ): Unit = alertService.deleteAlert(alertUuid, httpRequest.alertRequestContext())

  @Operation(hidden = true)
  @PostMapping("/inactive")
  fun publishInactiveAlertEvents() {
    alertService.publishInactiveTodayAlertEvents()
  }

  private fun HttpServletRequest.alertRequestContext() = getAttribute(AlertRequestContext::class.simpleName) as AlertRequestContext
}
