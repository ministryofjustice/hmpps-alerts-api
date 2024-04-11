package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class MigratedAlert(
  @Schema(
    description = "The internal NOMIS id for the offender booking. " +
      "An alert in NOMIS is uniquely identified by the offender booking id and alert sequence." +
      "This is returned as part of the migrated alert response for mapping between NOMIS and DPS.",
    example = "12345",
  )
  val offenderBookId: Long,

  @Schema(
    description = "The sequence of the NOMIS offender booking. " +
      "A sequence of 1 means the alert is from the current booking. A sequence of > 1 means the alert is from a historic booking." +
      "This is returned as part of the migrated alert response for mapping between NOMIS and DPS.",
    example = "1",
  )
  val bookingSeq: Int,

  @Schema(
    description = "The NOMIS alert sequence. " +
      "An alert in NOMIS is uniquely identified by the offender booking id and alert sequence." +
      "This is returned as part of the migrated alert response for mapping between NOMIS and DPS.",
    example = "2",
  )
  val alertSeq: Int,

  @Schema(
    description = "The unique identifier assigned to the alert",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val alertUuid: UUID,
)
