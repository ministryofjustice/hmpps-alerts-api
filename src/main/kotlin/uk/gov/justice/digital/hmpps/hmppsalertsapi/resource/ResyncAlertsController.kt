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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.ResyncedAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.ResyncAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.ResyncAlertsService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/resync/{prisonNumber}/alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class ResyncAlertsController(private val resyncAlertsService: ResyncAlertsService) {
  @Operation(summary = "Resync all alerts for a prisoner from NOMIS")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Resync of alerts successful",
        content = [Content(array = ArraySchema(schema = Schema(implementation = ResyncedAlert::class)))],
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
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI', '$ROLE_NOMIS_ALERTS')")
  fun resyncPrisonerAlerts(
    @PathVariable
    @Parameter(
      description = "Unique identifier of the prisoner. Aliases: offender number, prisoner number, offender id or NOMS id",
      example = "A1234AA",
      required = true,
    )
    prisonNumber: String,
    @Valid
    @RequestBody
    @Parameter(
      description = "The alert data to use to resync alerts in the service",
      required = true,
    )
    alerts: List<ResyncAlert>,
  ): List<ResyncedAlert> = resyncAlertsService.resyncAlerts(prisonNumber, alerts)
}
