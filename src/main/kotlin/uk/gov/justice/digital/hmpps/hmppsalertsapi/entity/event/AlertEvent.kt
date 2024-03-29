package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import java.time.LocalDateTime
import java.util.UUID

abstract class AlertEvent {
  abstract val alertUuid: UUID
  abstract val prisonNumber: String
  abstract val alertCode: String
  abstract val occurredAt: LocalDateTime
  abstract val source: Source

  abstract fun toDomainEvent(baseUrl: String): AlertDomainEvent

  protected fun toDomainEvent(type: DomainEventType, baseUrl: String): AlertDomainEvent =
    AlertDomainEvent(
      eventType = type.eventType,
      additionalInformation = AlertAdditionalInformation(
        url = "$baseUrl/alerts/$alertUuid",
        alertUuid = alertUuid,
        prisonNumber = prisonNumber,
        alertCode = alertCode,
        source = source,
      ),
      description = type.description,
      occurredAt = occurredAt,
    )
}

data class AlertCreatedEvent(
  override val alertUuid: UUID,
  override val prisonNumber: String,
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  val createdBy: String,
) : AlertEvent() {
  override fun toString(): String {
    return "Alert with UUID '$alertUuid' " +
      "created for prison number '$prisonNumber' " +
      "with alert code '$alertCode' " +
      "at '$occurredAt' " +
      "by '$createdBy' " +
      "from source '$source'."
  }

  override fun toDomainEvent(baseUrl: String): AlertDomainEvent =
    toDomainEvent(ALERT_CREATED, baseUrl)
}

data class AlertUpdatedEvent(
  override val alertUuid: UUID,
  override val prisonNumber: String,
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  val updatedBy: String,
  val descriptionUpdated: Boolean,
  val authorisedByUpdated: Boolean,
  val activeFromUpdated: Boolean,
  val activeToUpdated: Boolean,
  val commentAppended: Boolean,
) : AlertEvent() {
  override fun toString(): String {
    return "Alert with UUID '$alertUuid' " +
      "updated for prison number '$prisonNumber' " +
      "with alert code '$alertCode' " +
      "at '$occurredAt' " +
      "by '$updatedBy' " +
      "from source '$source'. " +
      "Properties updated: " +
      "description: $descriptionUpdated, " +
      "authorisedBy: $authorisedByUpdated, " +
      "activeFrom: $activeFromUpdated, " +
      "activeTo: $activeToUpdated, " +
      "comment appended: $commentAppended."
  }

  override fun toDomainEvent(baseUrl: String): AlertDomainEvent =
    toDomainEvent(ALERT_UPDATED, baseUrl)
}

data class AlertDeletedEvent(
  override val alertUuid: UUID,
  override val prisonNumber: String,
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  val deletedBy: String,
) : AlertEvent() {
  override fun toString(): String {
    return "Alert with UUID '$alertUuid' " +
      "deleted for prison number '$prisonNumber' " +
      "with alert code '$alertCode' " +
      "at '$occurredAt' " +
      "by '$deletedBy' " +
      "from source '$source'."
  }

  override fun toDomainEvent(baseUrl: String): AlertDomainEvent =
    toDomainEvent(ALERT_DELETED, baseUrl)
}
