package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ADMIN_UI_ONLY
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.RO_OPERATIONS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlertTypeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertTypeService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/alert-types", produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertTypesController(
  private val alertTypeService: AlertTypeService,
) {
  @Tag(name = RO_OPERATIONS)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RO', '$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
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
  @UsernameHeader
  fun retrieveAlertTypes(
    @Parameter(
      description = "Include inactive alert types and codes. Defaults to false",
    )
    includeInactive: Boolean = false,
  ): Collection<AlertType> = alertTypeService.getAlertTypes(includeInactive, AlertRequestContext.get())

  @Tag(name = RO_OPERATIONS)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RO', '$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @GetMapping("/{alertTypeCode}")
  @Operation(
    summary = "Get an alert type",
    description = "Returns the specified alert type.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert type found",
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
        responseCode = "404",
        description = "Not found, the alert type was is not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @UsernameHeader
  fun retrieveAlertType(@PathVariable alertTypeCode: String): AlertType = alertTypeService.getAlertType(alertTypeCode, AlertRequestContext.get())

  @Tag(name = ADMIN_UI_ONLY)
  @ResponseStatus(CREATED)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
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

  @Tag(name = ADMIN_UI_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @PatchMapping("/{alertType}/deactivate")
  @Operation(
    summary = "Deactivate an alert type",
    description = "Deactivate an alert type, typically from the Alerts UI",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert type deactivated",
        content = [Content(schema = Schema(implementation = AlertType::class))],
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
        description = "Not found, the alert type was is not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @UsernameHeader
  fun deactivateAlertType(
    @PathVariable alertType: String,
    httpRequest: HttpServletRequest,
  ) = alertTypeService.deactivateAlertType(alertType, httpRequest.alertRequestContext())

  @Tag(name = ADMIN_UI_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @PatchMapping("/{alertType}/reactivate")
  @Operation(
    summary = "Reactivate an alert type",
    description = "Reactivate an alert type, typically from the Alerts UI",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert type reactivated",
        content = [Content(schema = Schema(implementation = AlertType::class))],
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
        description = "Not found, the alert type was is not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @UsernameHeader
  fun reactivateAlertType(
    @PathVariable alertType: String,
    httpRequest: HttpServletRequest,
  ) = alertTypeService.reactivateAlertType(alertType, httpRequest.alertRequestContext())

  @Tag(name = ADMIN_UI_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @PatchMapping("/{alertType}")
  @Operation(
    summary = "Update alert type",
    description = "Set the properties of an alert type to the submitted value.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert type updated",
        content = [Content(schema = Schema(implementation = AlertType::class))],
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
        description = "Not found, the alert type was is not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @UsernameHeader
  fun updateAlertType(
    @PathVariable
    @Size(max = 12, min = 1, message = "Code must be between 1 & 12 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Code must only contain uppercase alphabetical and/or numeric characters")
    alertType: String,
    @Valid @RequestBody updateRequest: UpdateAlertTypeRequest,
    httpRequest: HttpServletRequest,
  ) = alertTypeService.updateAlertType(alertType, updateRequest, httpRequest.alertRequestContext())

  private fun HttpServletRequest.alertRequestContext() = getAttribute(AlertRequestContext::class.simpleName) as AlertRequestContext
}
