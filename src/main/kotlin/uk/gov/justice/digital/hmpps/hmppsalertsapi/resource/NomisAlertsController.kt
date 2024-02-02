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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert

@RestController
@RequestMapping("/nomis-alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class NomisAlertsController {
  @GetMapping("/{alertId}")
  @Operation(
    summary = "Get an alert in NOMIS format by its identifier",
    description = "Returns the alert with the matching identifier in the NOMIS data format. " +
      "This endpoint is intended to be used by the synchronisation process to retrieve the latest state of an alert.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert found",
        content = [Content(schema = Schema(implementation = NomisAlert::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = NomisAlert::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = NomisAlert::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The alert associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_NOMIS_ALERTS', '$ROLE_ALERTS_ADMIN')")
  fun retrieveAlert(
    @PathVariable
    @Parameter(
      description = "Alert identifier",
      required = true,
    )
    alertId: Long,
    request: HttpServletRequest,
  ): NomisAlert = throw NotImplementedError()

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Create an alert from NOMIS alert data",
    description = "Accepts alerts data in NOMIS format and creates a corresponding alert in this service. " +
      "The alert is returned along with the assigned alert id for mapping purposes. " +
      "This endpoint is idempotent and will not create duplicate alert records. It uses the supplied offender book id and sequence number values " +
      "as a combined unique identifier. If that combination is already in the data store, that alert will be returned using HTTP status code 200 OK. " +
      "This endpoint does not validate the supplied data as it must accept all NOMIS alert data. " +
      "This endpoint is intended to be used by the synchronisation process to migrate existing alerts and sync new ones.",
  )
  @ApiResponses(
    value = [
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
  @PreAuthorize("hasAnyRole('$ROLE_NOMIS_ALERTS', '$ROLE_ALERTS_ADMIN')")
  @SyncSuppressEventsHeader
  fun createAlert(
    @Valid
    @RequestBody
    @Parameter(
      description = "The NOMIS alert to create in this service",
      required = true,
    )
    nomisAlert: NomisAlert,
  ): Alert = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @PutMapping("/{alertId}")
  @Operation(
    summary = "Updates an existing alert with new NOMIS alert data",
    description = "Accepts alerts data in NOMIS format and updates the alert with the corresponding identifier in this service. " +
      "This endpoint does not validate the supplied data as it must accept all NOMIS alert data. " +
      "This endpoint is intended to be used by the synchronisation process to sync updates to existing alerts.",
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
      ApiResponse(
        responseCode = "404",
        description = "The alert associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_NOMIS_ALERTS', '$ROLE_ALERTS_ADMIN')")
  fun updateAlert(
    @PathVariable
    @Parameter(
      description = "Alert identifier",
      required = true,
    )
    alertId: Long,
    @Valid
    @RequestBody
    @Parameter(
      description = "The NOMIS alert to create in this service",
      required = true,
    )
    nomisAlert: NomisAlert,
  ): Alert = throw NotImplementedError()

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{alertId}")
  @Operation(
    summary = "Deletes and alert",
    description = "Deletes an alert. This endpoint fully removes the alert from the system. It is used when an alert " +
      "has been created in error or otherwise removed from NOMIS and should therefore be removed from this service. " +
      "This endpoint is intended to be used by the synchronisation process to sync the deletion of existing alerts.",
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
        description = "The alert associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_NOMIS_ALERTS', '$ROLE_ALERTS_ADMIN')")
  fun deleteAlert(
    @PathVariable
    @Parameter(
      description = "Alert identifier",
      required = true,
    )
    alertId: Long,
  ): Unit = throw NotImplementedError()
}
