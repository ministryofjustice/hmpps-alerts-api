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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.NomisAlertMapping
import java.util.UUID

@RestController
@RequestMapping("/nomis-alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class NomisAlertsController {
  @GetMapping("/{alertUuid}")
  @Operation(
    summary = "Get an alert in NOMIS format by its unique identifier. SYNC ONLY",
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
      description = "Alert unique identifier",
      required = true,
    )
    alertUuid: UUID,
    request: HttpServletRequest,
  ): NomisAlert = throw NotImplementedError()

  @PostMapping
  @Operation(
    summary = "Create or update an alert from NOMIS alert data. An upsert endpoint. SYNC ONLY",
    description = "Accepts alerts data in NOMIS format and creates a corresponding alert in this service if one does not exist or " +
      "updates an existing alert if a corresponding alert already exists in the new service. This can be disambiguated by the response code, " +
      "A 201 Created indicates the request caused a new alert to be created, a 200 OK is returned when an existing alert was updated by the request. " +
      "In both cases, the alert's unique identifier is returned for mapping purposes. " +
      "This endpoint is therefore idempotent and will not create duplicate alert records. " +
      "This endpoint does not validate the supplied data as it must accept all NOMIS alert data. " +
      "This endpoint is intended to be used by the synchronisation process to migrate existing alerts and sync new and updated ones. " +
      "The '$SYNC_SUPPRESS_EVENTS' header should be used to suppress domain event publishing when alerts are initially migrated. " +
      "It should not be used or should be set to false (the default) when syncing new and updated alerts. ",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert updated successfully",
        content = [Content(schema = Schema(implementation = NomisAlertMapping::class))],
      ),
      ApiResponse(
        responseCode = "201",
        description = "Alert created successfully",
        content = [Content(schema = Schema(implementation = NomisAlertMapping::class))],
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
      description = "The NOMIS alert to create or update in this service",
      required = true,
    )
    nomisAlert: NomisAlert,
  ): NomisAlertMapping = throw NotImplementedError()

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{alertUuid}")
  @Operation(
    summary = "Deletes an alert. SYNC ONLY",
    description = "This endpoint fully removes the alert from the system. It is used when an alert " +
      "has been created in error or otherwise removed from NOMIS and should therefore also be removed from this service. " +
      "This endpoint will return 200 OK if the alert was not found or already deleted. This is to prevent low value warnings being logged. " +
      "This endpoint is intended to be used by the synchronisation process to sync the deletion of existing alerts.",
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
  @PreAuthorize("hasAnyRole('$ROLE_NOMIS_ALERTS', '$ROLE_ALERTS_ADMIN')")
  fun deleteAlert(
    @PathVariable
    @Parameter(
      description = "Alert unique identifier",
      required = true,
    )
    alertUuid: UUID,
  ): Unit = throw NotImplementedError()
}
