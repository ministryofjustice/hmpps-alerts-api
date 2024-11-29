package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkAlertPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.PlanBulkAlert
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping("/bulk-alerts", produces = [MediaType.APPLICATION_JSON_VALUE])
class BulkAlertsController(private val plan: PlanBulkAlert) {

  @PostMapping("/plan")
  @Operation(summary = "Create the plan for bulk alerts")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Alerts creation plan generated successfully",
        content = [Content(schema = Schema(implementation = BulkAlertPlan::class))],
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
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @UsernameHeader
  @SourceHeader
  fun createPlan(): BulkPlan = plan.createNew()

  @PatchMapping("/plan/{id}")
  @Operation(summary = "Update the plan for bulk alerts")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alerts creation plan generated successfully",
        content = [Content(schema = Schema(implementation = BulkAlertPlan::class))],
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
  @PreAuthorize("hasAnyRole('$ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI')")
  @UsernameHeader
  @SourceHeader
  fun updatePlan(@PathVariable id: UUID, @RequestBody @NotEmpty @Valid actions: Set<BulkAction>): BulkPlan =
    plan.update(id, actions)
}
