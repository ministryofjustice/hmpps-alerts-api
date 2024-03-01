package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

abstract class AlertEvent {
  abstract val alertUuid: UUID
  abstract val prisonNumber: String
  abstract val alertCode: String
  abstract val occurredAt: LocalDateTime
  abstract val source: Source
}

data class AlertCreatedEvent(
  override val alertUuid: UUID,
  override val prisonNumber: String,
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  val createdBy: String,
) : AlertEvent()
