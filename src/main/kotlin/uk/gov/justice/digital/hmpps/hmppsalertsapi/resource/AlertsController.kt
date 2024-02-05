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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import java.util.UUID

@RestController
@RequestMapping("/alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertsController {
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
        content = [Content(schema = Schema(implementation = Alert::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = Alert::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The alert associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_ALERTS_READER', '$ROLE_ALERTS_ADMIN')")
  fun retrieveAlert(
    @PathVariable
    @Parameter(
      description = "Alert unique identifier",
      required = true,
    )
    alertUuid: UUID,
    request: HttpServletRequest,
  ): Alert = throw NotImplementedError()

  @PostMapping
  @Operation(
    summary = "Create or update an alert. An upsert endpoint",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert updated successfully",
        content = [Content(schema = Schema(implementation = Alert::class))],
      ),
      ApiResponse(
        responseCode = "201",
        description = "Alert created successfully",
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
  @PreAuthorize("hasAnyRole('$ROLE_ALERTS_WRITER', '$ROLE_ALERTS_ADMIN')")
  @SyncSuppressEventsHeader
  fun createAlert(
    @Valid
    @RequestBody
    @Parameter(
      description = "The alert to create or update in this service",
      required = true,
    )
    alert: Alert,
  ): Alert = throw NotImplementedError()

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{alertUuid}")
  @Operation(
    summary = "Deletes an alert",
    description = "This endpoint fully removes the alert from the system. It is used when an alert " +
      "has been created in error or should otherwise be removed from this service. " +
      "This endpoint will return 200 OK if the alert was not found or already deleted. This is to prevent low value warnings being logged.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert was not found or already deleted",
      ),
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
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_ALERTS_WRITER', '$ROLE_ALERTS_ADMIN')")
  fun deleteAlert(
    @PathVariable
    @Parameter(
      description = "Alert unique identifier",
      required = true,
    )
    alertUuid: UUID,
  ): Unit = throw NotImplementedError()
}
