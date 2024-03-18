package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description = "The summary information of an alert code used to categorise alerts",
)
data class AlertCodeSummary(
  @Schema(
    description = "The short code for the alert type",
    example = "A",
  )
  val alertTypeCode: String,

  @Schema(
    description = "The description of the alert type",
    example = "Alert type description",
  )
  val alertTypeDescription: String,

  @Schema(
    description = "The short code for the alert code. Usually starts with the alert type code",
    example = "ABC",
  )
  val code: String,

  @Schema(
    description = "The description of the alert code",
    example = "Alert code description",
  )
  val description: String,
)
