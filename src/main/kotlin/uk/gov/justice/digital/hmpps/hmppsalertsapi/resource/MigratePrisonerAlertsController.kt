package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MigratedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MigrateAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.MigrateAlertService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/migrate/{prisonNumber}/alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class MigratePrisonerAlertsController(
  private val migrateAlertService: MigrateAlertService,
) {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Migrate all alerts for a prisoner from NOMIS",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Alerts migrated successfully",
        content = [Content(array = ArraySchema(schema = Schema(implementation = MigratedAlert::class)))],
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
  @PreAuthorize("hasAnyRole('$ROLE_ALERTS_ADMIN', '$ROLE_NOMIS_ALERTS')")
  fun createAlert(
    @PathVariable
    @Parameter(
      description = "Prison number of the prisoner. Also referred to as the offender number, offender id or NOMS id",
      example = "A1234AA",
      required = true,
    )
    prisonNumber: String,
    @Valid
    @RequestBody
    @Parameter(
      description = "The alert data to use to create an alert in the service",
      required = true,
    )
    request: List<MigrateAlert>,
  ): List<MigratedAlert> = migrateAlertService.migratePrisonerAlerts(prisonNumber, request)
}
