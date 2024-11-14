package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.constraints.NotEmpty
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertsResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/search/alerts")
class SearchController(private val alertService: AlertService) {
  @Operation(
    summary = "Gets all the alerts for prisoners by their prison numbers",
    description = "Returns all the alerts for the supplied prison numbers.",
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
  @PostMapping("/prison-numbers")
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__RO', '$ROLE_PRISONER_ALERTS__RW', '$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  fun retrievePrisonerAlerts(
    @RequestBody @NotEmpty(message = "Prison numbers must not be empty") prisonNumbers: Set<String>,
    @RequestParam(required = false, defaultValue = "false") includeInactive: Boolean,
  ): AlertsResponse = alertService.retrieveAlertsForPrisonNumbers(prisonNumbers, includeInactive)
}
