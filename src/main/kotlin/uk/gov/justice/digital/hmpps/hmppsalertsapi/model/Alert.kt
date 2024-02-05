package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class Alert(
  @Schema(
    description = "The unique identifier assigned to the alert",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val alertUuid: UUID,
)
