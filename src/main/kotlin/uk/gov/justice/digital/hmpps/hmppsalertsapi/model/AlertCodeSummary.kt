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
    description = "The short code for the alert code. Usually starts with the alert type code",
    example = "ABC",
  )
  val code: String,

  @Schema(
    description = "The description of the alert code",
    example = "Alert code description",
  )
  val description: String,

  @Schema(
    description = "The sequence number of the alert code within the alert type. " +
      "Used for ordering alert codes correctly in lists and drop downs. " +
      "A value of 0 indicates this is the default alert code for the alert type",
    example = "3",
  )
  val listSequence: Int,

  @Schema(
    description = "Indicates that the alert code is active and can be used. " +
      "Inactive alert codes are not returned by default in the API",
    example = "true",
  )
  val isActive: Boolean,
)
