package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class ResyncedAlert(
  @Schema(
    description = "The internal NOMIS id for the offender booking. " +
      "An alert in NOMIS is uniquely identified by the offender booking id and alert sequence." +
      "This is returned as part of the resync alert response for mapping between NOMIS and DPS.",
    example = "12345",
  )
  val offenderBookId: Long,

  @Schema(
    description = "The NOMIS alert sequence. " +
      "An alert in NOMIS is uniquely identified by the offender booking id and alert sequence." +
      "This is returned as part of the resync alert response for mapping between NOMIS and DPS.",
    example = "2",
  )
  val alertSeq: Int,

  @Schema(
    description = "The unique identifier assigned to the alert",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val alertUuid: UUID,
)
