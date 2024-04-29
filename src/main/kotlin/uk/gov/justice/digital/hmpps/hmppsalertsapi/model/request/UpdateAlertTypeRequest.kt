package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(
  description = "The request body for updating the properties of an alert type",
)
data class UpdateAlertTypeRequest(
  @Schema(
    description = "The new property value(s) to be updated onto an alert type",
    example = "New description value for an alert type",
  )
  @field:Size(max = 40, min = 1, message = "Description must be between 1 & 40 characters")
  val description: String,
)
