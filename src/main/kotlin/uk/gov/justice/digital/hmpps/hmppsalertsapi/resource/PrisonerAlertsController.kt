package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
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
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RO', '$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
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
    @PageableDefault(sort = ["activeFrom"], direction = Direction.DESC, size = Int.MAX_VALUE)
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
    description = "Returns all the alerts for the supplied prison numbers. The alerts for each prisoner are mapped to their prison number.",
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
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RO', '$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  fun retrievePrisonerAlerts(
    @RequestParam
    @Parameter(
      description = "The prison numbers of the prisoners",
      required = true,
    )
    prisonNumbers: Collection<String>,
  ): Map<String, List<Alert>> = alertService.retrieveAlertsForPrisonNumbers(prisonNumbers)

  @Operation(
    summary = "Create an alert",
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
      ApiResponse(
        responseCode = "409",
        description = "Conflict, the person already has an active alert using the supplied alert code",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/{prisonNumber}/alerts")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @UsernameHeader
  @SourceHeader
  fun createPrisonerAlert(
    @PathVariable
    @Parameter(
      description = "Prison number of the prisoner. Also referred to as the offender number, offender id or NOMS id",
      example = "A1234AA",
      required = true,
    )
    prisonNumber: String,
    @Valid
    @RequestBody
    @Parameter(description = "The alert data to use to create an alert in the service", required = true)
    request: CreateAlert,
    @Parameter(
      description = "Allows the creation of an alert using an inactive code. Intended only for use by the Alerts UI when the user has the ‘Manage Alerts in Bulk for Prison Estate’ role. Defaults to false",
    )
    allowInactiveCode: Boolean = false,
  ): Alert {
    return alertService.createAlert(prisonNumber, request, allowInactiveCode)
  }
}
