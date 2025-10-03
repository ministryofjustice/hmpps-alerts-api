package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ADMIN_ONLY
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertCodePrivilegedUserService
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertCodeService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/alert-codes/{alertCode}", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
class AlertCodeRestrictionsController(
  private val alertCodeService: AlertCodeService,
  private val alertCodePrivilegedUserService: AlertCodePrivilegedUserService,
) {
  @Tag(name = ADMIN_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/restrict")
  @Operation(
    summary = "Restrict an alert code",
    description = "Restrict an alert code to be administered only by named users",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert code restricted",
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
  fun restrictAlertCode(
    @PathVariable alertCode: String,
    httpRequest: HttpServletRequest,
  ) = alertCodeService.setAlertCodeRestrictedStatus(alertCode, true, httpRequest.alertRequestContext())

  @Tag(name = ADMIN_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/remove-restriction")
  @Operation(
    summary = "Remove restriction on an alert code",
    description = "Remove restriction an alert code to so that all users can administer it",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert code restriction removed",
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
  fun removeAlertCodeRestriction(
    @PathVariable alertCode: String,
    httpRequest: HttpServletRequest,
  ) = alertCodeService.setAlertCodeRestrictedStatus(alertCode, false, httpRequest.alertRequestContext())

  @Tag(name = ADMIN_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @PostMapping("/privileged-user/{username}")
  @Operation(
    summary = "Add a privileged user for an alert code",
    description = "Add a privileged user for an alert code. If the alert code is restricted this user will still be able to administer it",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Privileged user added",
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
  fun addPrivilegedUser(
    @PathVariable alertCode: String,
    @PathVariable username: String,
  ) = alertCodePrivilegedUserService.addPrivilegedUser(alertCode, username)

  @Tag(name = ADMIN_ONLY)
  @ResponseStatus(HttpStatus.OK)
  @DeleteMapping("/privileged-user/{username}")
  @Operation(
    summary = "Remove a privileged user for an alert code",
    description = "Remove a privileged user for an alert code. This user will no longer be able to administer the alert code if it is restricted",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Privileged user added",
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
  fun removePrivilegedUser(
    @PathVariable alertCode: String,
    @PathVariable username: String,
  ) = alertCodePrivilegedUserService.removePrivilegedUser(alertCode, username)
}
