package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.AlertTypeService

@RestController
@RequestMapping("/alert-types", produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertTypesController(
  private val alertTypeService: AlertTypeService,
) {
  @GetMapping
  @Operation(
    summary = "Get all alert types",
    description = "Returns the full list of alert types and the alert codes within those types. " +
      "By default this endpoint only returns active alert types and codes. " +
      "The include inactive parameter can be used to return all alert types and codes.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Alert types and codes found",
        content = [Content(array = ArraySchema(schema = Schema(implementation = AlertType::class)))],
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
  fun getAlertTypes(
    @Parameter(
      description = "Include inactive alert types and codes. Defaults to false",
    )
    includeInactive: Boolean = false,
  ): Collection<AlertType> = alertTypeService.getAlertTypes(includeInactive)
}