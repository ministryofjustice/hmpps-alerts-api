package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CODE_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CODE_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_INACTIVE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_TYPE_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import java.time.LocalDateTime
import java.util.UUID

sealed interface DomainEventable {
  val type: DomainEventType
  fun detailPath(): String
  fun toDomainEvent(baseUrl: String): DomainEvent
}

sealed interface AlertEvent : DomainEventable {
  val alertUuid: UUID
  val prisonNumber: String
  val alertCode: String
  val occurredAt: LocalDateTime
  val source: Source
  override fun detailPath(): String = "/alerts/$alertUuid"
  override fun toDomainEvent(baseUrl: String): AlertDomainEvent<AlertAdditionalInformation> =
    AlertDomainEvent(
      eventType = type.eventType,
      additionalInformation = AlertAdditionalInformation(
        alertUuid = alertUuid,
        alertCode = alertCode,
        source = source,
      ),
      description = type.description,
      occurredAt = occurredAt.toZoneDateTime(),
      detailUrl = "$baseUrl${detailPath()}",
      personReference = PersonReference.withPrisonNumber(prisonNumber),
    )
}

data class AlertCreatedEvent(
  override val alertUuid: UUID,
  override val prisonNumber: String,
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  val createdBy: String,
) : AlertEvent {
  override val type: DomainEventType = ALERT_CREATED
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
) : AlertEvent {
  override val type: DomainEventType = ALERT_UPDATED
}

data class AlertDeactivatedEvent(
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
) : AlertEvent {
  override val type: DomainEventType = ALERT_INACTIVE
}

data class AlertDeletedEvent(
  override val alertUuid: UUID,
  override val prisonNumber: String,
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
  override val source: Source,
  val deletedBy: String,
) : AlertEvent {
  override val type: DomainEventType = ALERT_DELETED
}

sealed interface AlertReferenceDataEvent : DomainEventable {
  val alertCode: String
  val occurredAt: LocalDateTime
  override fun toDomainEvent(
    baseUrl: String,
  ): AlertDomainEvent<ReferenceDataAdditionalInformation> =
    AlertDomainEvent(
      eventType = type.eventType,
      additionalInformation = ReferenceDataAdditionalInformation(
        alertCode = alertCode,
        source = DPS,
      ),
      description = type.description,
      occurredAt = occurredAt.toZoneDateTime(),
      detailUrl = "$baseUrl${detailPath()}",
    )
}

sealed interface AlertCodeEvent : AlertReferenceDataEvent {
  override fun detailPath(): String = "/alert-codes/$alertCode"
}

data class AlertCodeCreatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertCodeEvent {
  override val type: DomainEventType = ALERT_CODE_CREATED
}

data class AlertCodeDeactivatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertCodeEvent {
  override val type: DomainEventType = DomainEventType.ALERT_CODE_DEACTIVATED
}

data class AlertCodeReactivatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertCodeEvent {
  override val type: DomainEventType = DomainEventType.ALERT_CODE_REACTIVATED
}

data class AlertCodeUpdatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertCodeEvent {
  override val type: DomainEventType = ALERT_CODE_UPDATED
}

sealed interface AlertTypeEvent : AlertReferenceDataEvent {
  override fun detailPath(): String = "/alert-types/$alertCode"
}

data class AlertTypeCreatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertTypeEvent {
  override val type: DomainEventType = ALERT_TYPE_CREATED
}

data class AlertTypeDeactivatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertTypeEvent {
  override val type: DomainEventType = DomainEventType.ALERT_TYPE_DEACTIVATED
}

data class AlertTypeReactivatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertTypeEvent {
  override val type: DomainEventType = DomainEventType.ALERT_TYPE_REACTIVATED
}

data class AlertTypeUpdatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertTypeEvent {
  override val type: DomainEventType = DomainEventType.ALERT_TYPE_UPDATED
}
