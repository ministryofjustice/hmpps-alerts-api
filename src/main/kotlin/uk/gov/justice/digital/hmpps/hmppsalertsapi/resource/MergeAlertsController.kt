package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.MergeAlerts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.MergeAlertService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/merge-alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class MergeAlertsController(
  private val mergeAlertService: MergeAlertService,
) {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Merge alerts associated with two prison numbers",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Alerts merged successfully",
        content = [Content(schema = Schema(implementation = MergedAlerts::class))],
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
    @Valid
    @RequestBody
    @Parameter(
      description = "The merge request data containing the from and to prison numbers and new alerts to create in the service",
      required = true,
    )
    request: MergeAlerts,
  ): MergedAlerts = mergeAlertService.mergePrisonerAlerts(request)
}
