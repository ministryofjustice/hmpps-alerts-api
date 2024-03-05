package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

abstract class DomainEvent<T : AdditionalInformation> {
  abstract val eventType: String
  abstract val additionalInformation: T
  abstract val version: Int
  abstract val description: String
  abstract val occurredAt: LocalDateTime

  override fun toString(): String {
    return "v$version domain event '$eventType' " +
      "for resource '${additionalInformation.url}' " +
      "from source '${additionalInformation.source}'"
  }
}

abstract class AdditionalInformation {
  abstract val url: String
  abstract val source: Source
}

data class AlertDomainEvent(
  override val eventType: String,
  override val additionalInformation: AlertAdditionalInformation,
  override val version: Int = 1,
  override val description: String,
  override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent<AlertAdditionalInformation>() {
  override fun toString(): String {
    return "v$version alert domain event '$eventType' " +
      "for alert with UUID '${additionalInformation.alertUuid}' " +
      "for prison number '${additionalInformation.prisonNumber}' " +
      "with alert code '${additionalInformation.alertCode}' " +
      "from source '${additionalInformation.source}'"
  }
}

data class AlertAdditionalInformation(
  override val url: String,
  val alertUuid: UUID,
  val prisonNumber: String,
  val alertCode: String,
  override val source: Source,
) : AdditionalInformation()
