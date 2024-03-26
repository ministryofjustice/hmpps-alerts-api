package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertTypeService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/alert-types", produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertTypesController(
  private val alertTypeService: AlertTypeService,
) {
  @PreAuthorize("hasAnyRole('$ROLE_ALERTS_READER', '$ROLE_ALERTS_ADMIN', '$PRISON')")
  @GetMapping
  @Operation(
    summary = "Get all alert types",
    description = "Returns the full list of alert types and the alert codes within those types. " +
      "By default this endpoint only returns active alert types and codes. " +
      "The include inactive parameter can be used to return all alert types and codes.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert types and codes found",
        content = [Content(array = ArraySchema(schema = Schema(implementation = AlertType::class)))],
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
  fun retrieveAlertTypes(
    @Parameter(
      description = "Include inactive alert types and codes. Defaults to false",
    )
    includeInactive: Boolean = false,
  ): Collection<AlertType> = alertTypeService.getAlertTypes(includeInactive)

  @ResponseStatus(CREATED)
  @PreAuthorize("hasAnyRole('$ROLE_ALERTS_ADMIN')")
  @PostMapping
  @Operation(
    summary = "Create an alert type",
    description = "Create a new alert type, typically from the Alerts UI",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Alert type created",
        content = [Content(array = ArraySchema(schema = Schema(implementation = AlertType::class)))],
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
        responseCode = "409",
        description = "Conflict, the alert type code already exists",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @UsernameHeader
  fun createAlertType(
    @Valid @RequestBody createAlertTypeRequest: CreateAlertTypeRequest,
    httpRequest: HttpServletRequest,
  ): AlertType = alertTypeService.createAlertType(createAlertTypeRequest, httpRequest.alertRequestContext())

  private fun HttpServletRequest.alertRequestContext() =
    getAttribute(AlertRequestContext::class.simpleName) as AlertRequestContext
}
