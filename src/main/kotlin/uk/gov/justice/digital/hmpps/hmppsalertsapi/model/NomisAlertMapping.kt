package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.UpsertStatus
import java.util.UUID

@Schema(
  description = "The mapping between a NOMIS alert and an alert in the HMPPS Alerts API. " +
    "NOMIS uses a composite key of offender book ID and alert sequence number to uniquely identify an alert. " +
    "The alert UUID is the unique identifier assigned to the alert in the HMPPS Alerts API.",
)
data class NomisAlertMapping(
  val offenderBookId: Long,

  val alertSeq: Int,

  @Schema(
    description = "The unique identifier assigned to the alert",
    example = "8cdadcf3-b003-4116-9956-c99bd8df6a00",
  )
  val alertUuid: UUID,

  val status: UpsertStatus,
)
