package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/prisoner/{prisonNumber}/alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerAlertsController(val alertService: AlertService) {
  @ResponseStatus(HttpStatus.OK)
  @GetMapping()
  @Operation(
    summary = "Gets all the alerts for a prisoner by their prison number",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alerts found",
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
  @PreAuthorize("hasAnyRole('$ROLE_ALERTS_READER', '$ROLE_ALERTS_ADMIN', '$PRISON')")
  fun retrievePrisonerAlerts(
    @PathVariable
    @Parameter(
      description = "Prison number of the prisoner. Also referred to as the offender number, offender id or NOMS id",
      example = "A1234AA",
      required = true,
    )
    prisonNumber: String,
    @RequestParam
    @Parameter(
      description = "Return only active (true) or inactive (false) alerts. If not provided or a null value is supplied, all alerts are returned",
      example = "true",
    )
    isActive: Boolean?,
    @RequestParam
    @Parameter(
      description = "Filter by alert type code or codes. Supply a comma separated list of alert type codes to filter by more than one code",
      example = "M",
    )
    alertType: String?,
    @ParameterObject
    @PageableDefault(sort = ["activeFrom"], direction = Direction.DESC)
    pageable: Pageable,
  ): Page<Alert> = alertService.retrieveAlertsForPrisonNumber(prisonNumber, isActive, alertType, pageable)
}
