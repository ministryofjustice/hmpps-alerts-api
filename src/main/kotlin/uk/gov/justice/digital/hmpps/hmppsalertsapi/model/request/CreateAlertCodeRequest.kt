package uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(
  description = "The request body for creating a new alert code",
)
data class CreateAlertCodeRequest(
  @Schema(
    description = "The short code for the alert code",
    example = "A",
  )
  @field:Size(max = 12, min = 1, message = "Code must be between 1 & 12 characters")
  @field:Pattern(regexp = "^[\\w #&'+\\-./<=>]+\$|^$", message = "Code must only contain alphanumeric characters and the following symbols: # & ' + \\ - . / < = >")
  val code: String,

  @Schema(
    description = "The description of the alert code",
    example = "Alert code description",
  )
  @field:Size(max = 40, min = 1, message = "Description must be between 1 & 40 characters")
  val description: String,

  @Schema(
    description = "The short code for the parent type",
    example = "A",
  )
  @field:Size(max = 12, min = 1, message = "Code must be between 1 & 12 characters")
  val parent: String,
)
