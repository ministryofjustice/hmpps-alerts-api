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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.response.PrisonersAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

@RestController
@RequestMapping("/prisoners", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerAlertsController(val alertService: AlertService) {
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/{prisonNumber}/alerts")
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
    @RequestParam
    @Parameter(
      description = "Filter by alert code or codes. Supply a comma separated list of alert codes to filter by more than one code",
      example = "AS",
    )
    alertCode: String?,
    @RequestParam
    @Parameter(
      description = "Filter alerts that have an active on date or after the supplied date",
      example = "2023-09-27",
    )
    activeFromStart: LocalDate?,
    @RequestParam
    @Parameter(
      description = "Filter alerts that have an active on date up to or before the supplied date",
      example = "2021-11-15",
    )
    activeFromEnd: LocalDate?,
    @RequestParam
    @Parameter(
      description = "Filter alerts that contain the search text in their description, authorised by or comments. The search is case insensitive and will match any part of the description, authorised by or comment text",
      example = "Search text",
    )
    search: String?,
    @ParameterObject
    @PageableDefault(sort = ["activeFrom"], direction = Direction.DESC)
    pageable: Pageable,
  ): Page<Alert> = alertService.retrieveAlertsForPrisonNumber(
    prisonNumber = prisonNumber,
    isActive = isActive,
    alertType = alertType,
    alertCode = alertCode,
    activeFromStart = activeFromStart,
    activeFromEnd = activeFromEnd,
    search = search,
    pageable = pageable,
  )

  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/alerts")
  @Operation(
    summary = "Gets all the alerts for prisoners by their prison numbers",
    description = "Returns all the alerts for the supplied prison numbers. The alerts are returned along with counts.",
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
    @RequestParam
    @Parameter(
      description = "The prison numbers of the prisoners",
      required = true,
    )
    prisonNumbers: Collection<String>,
  ): PrisonersAlerts = alertService.retrieveAlertsForPrisonNumbers(prisonNumbers)
}
