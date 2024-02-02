package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class Alert(
  @Schema(
    description = "The identifier assigned to the alert",
    example = "12345",
  )
  val alertId: Long,
)
