package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(
  description = "The request body for creating a new alert type",
)
data class CreateAlertTypeRequest(
  @Schema(
    description = "The short code for the alert type",
    example = "A",
  )
  @field:Size(max = 12, min = 1, message = "Code must be between 1 & 12 characters")
  val code: String,

  @Schema(
    description = "The description of the alert type",
    example = "Alert type description",
  )
  @field:Size(max = 40, min = 1, message = "Description must be between 1 & 40 characters")
  val description: String,
)
