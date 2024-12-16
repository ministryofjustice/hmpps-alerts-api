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
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlertCodeRequest
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertCodeService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/alert-codes", produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertCodesController(
  private val alertCodeService: AlertCodeService,
) {

  @Tag(name = ADMIN_UI_ONLY)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
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

  @Tag(name = ADMIN_UI_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @PatchMapping("/{alertCode}/deactivate")
  @Operation(
    summary = "Deactivate an alert code",
    description = "Deactivate an alert code, typically from the Alerts UI",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert code deactivated",
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
        description = "Not found, the alert code was is not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @UsernameHeader
  fun deactivateAlertCode(
    @PathVariable alertCode: String,
    httpRequest: HttpServletRequest,
  ) = alertCodeService.deactivateAlertCode(alertCode, httpRequest.alertRequestContext())

  @Tag(name = ADMIN_UI_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @PatchMapping("/{alertCode}/reactivate")
  @Operation(
    summary = "Reactivate an alert code",
    description = "Reactivate an alert code, typically from the Alerts UI",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert code reactivated",
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
        description = "Not found, the alert code was is not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @UsernameHeader
  fun reactivateAlertCode(
    @PathVariable alertCode: String,
    httpRequest: HttpServletRequest,
  ) = alertCodeService.reactivateAlertCode(alertCode, httpRequest.alertRequestContext())

  @Tag(name = RO_OPERATIONS)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RO', '$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @GetMapping
  @Operation(
    summary = "Retrieve all alert codes",
    description = "Retrieve all alert codes, typically from the Alerts UI",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert code retrieved",
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
  fun retrieveAlertCodes(
    @Parameter(
      description = "Include inactive alert types and codes. Defaults to false",
    )
    includeInactive: Boolean = false,
  ) = alertCodeService.retrieveAlertCodes(includeInactive)

  @Tag(name = RO_OPERATIONS)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RO', '$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @GetMapping("/{alertCode}")
  @Operation(
    summary = "Retrieve an alert code",
    description = "Retrieve an alert code, typically from the Alerts UI",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert code retrieved",
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
        description = "Not found, the alert code was is not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun retrieveAlertCode(
    @PathVariable alertCode: String,
    httpRequest: HttpServletRequest,
  ) = alertCodeService.retrieveAlertCode(alertCode)

  @Tag(name = ADMIN_UI_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @PatchMapping("/{alertCode}")
  @Operation(
    summary = "Update alert code",
    description = "Set the properties of an alert code to the submitted value.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert code updated",
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
        description = "Not found, the alert code was is not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @UsernameHeader
  fun updateAlertCode(
    @PathVariable
    @Size(max = 12, min = 1, message = "Code must be between 1 & 12 characters")
    @Pattern(
      regexp = "^[A-Z0-9]+$",
      message = "Code must only contain uppercase alphabetical and/or numeric characters",
    )
    alertCode: String,
    @Valid @RequestBody updateRequest: UpdateAlertCodeRequest,
    httpRequest: HttpServletRequest,
  ) = alertCodeService.updateAlertCode(alertCode, updateRequest, httpRequest.alertRequestContext())

  private fun HttpServletRequest.alertRequestContext() =
    getAttribute(AlertRequestContext::class.simpleName) as AlertRequestContext
}
