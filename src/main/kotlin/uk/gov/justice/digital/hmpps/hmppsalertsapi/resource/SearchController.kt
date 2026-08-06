package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.RO_OPERATIONS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertsResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/search/alerts")
class SearchController(private val alertService: AlertService) {

  @Tag(name = RO_OPERATIONS)
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
  @UsernameHeader
  fun retrievePrisonerAlerts(
    @RequestBody
    @NotEmpty(message = "Prison numbers must not be empty")
    prisonNumbers: Set<String>,
    @RequestParam(required = false, defaultValue = "false")
    @Parameter(description = "Whether to include inactive alerts")
    includeInactive: Boolean,
    @RequestParam(required = false)
    @Size(min = 1, message = "When provided, filterAlertCodes must include at least one code")
    @Parameter(description = "Whether to filter for only given alert codes (all by default)")
    filterAlertCodes: Set<String>? = null,
  ): AlertsResponse = alertService.retrieveAlertsForPrisonNumbers(prisonNumbers, includeInactive, filterAlertCodes, AlertRequestContext.get())
}
