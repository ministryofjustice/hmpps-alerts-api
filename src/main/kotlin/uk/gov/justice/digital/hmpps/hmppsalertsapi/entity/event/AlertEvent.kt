package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERTS_MERGED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlert
import java.time.LocalDateTime
import java.util.UUID

abstract class AlertEvent {
  abstract val alertUuid: UUID
  abstract val prisonNumber: String
  abstract val alertCode: String
  abstract val occurredAt: LocalDateTime
  abstract val source: Source

  abstract fun toDomainEvent(baseUrl: String): AlertDomainEvent<AlertAdditionalInformation>

  protected fun toDomainEvent(type: DomainEventType, baseUrl: String): AlertDomainEvent<AlertAdditionalInformation> =
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
      occurredAt = occurredAt.toZoneDateTime(),
      detailUrl = "$baseUrl/alerts/$alertUuid",
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
) : AlertEvent() {
  override fun toString(): String {
    return "Alert with UUID '$alertUuid' " +
      "created for prison number '$prisonNumber' " +
      "with alert code '$alertCode' " +
      "at '$occurredAt' " +
      "by '$createdBy' " +
      "from source '$source' "
  }

  override fun toDomainEvent(baseUrl: String) = toDomainEvent(ALERT_CREATED, baseUrl)
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
      "from source '$source' " +
      "Properties updated: " +
      "description: $descriptionUpdated, " +
      "authorisedBy: $authorisedByUpdated, " +
      "activeFrom: $activeFromUpdated, " +
      "activeTo: $activeToUpdated, " +
      "comment appended: $commentAppended."
  }

  override fun toDomainEvent(baseUrl: String) = toDomainEvent(ALERT_UPDATED, baseUrl)
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
      "from source '$source' "
  }

  override fun toDomainEvent(baseUrl: String) = toDomainEvent(ALERT_DELETED, baseUrl)
}

abstract class AlertCodeEvent {
  abstract val alertCode: String
  abstract val occurredAt: LocalDateTime

  abstract fun toDomainEvent(baseUrl: String): AlertDomainEvent<ReferenceDataAdditionalInformation>

  protected fun toDomainEvent(
    type: DomainEventType,
    baseUrl: String,
  ): AlertDomainEvent<ReferenceDataAdditionalInformation> =
    AlertDomainEvent(
      eventType = type.eventType,
      additionalInformation = ReferenceDataAdditionalInformation(
        url = "$baseUrl/alert-codes/$alertCode",
        alertCode = alertCode,
        source = DPS,
      ),
      description = type.description,
      occurredAt = occurredAt.toZoneDateTime(),
      detailUrl = "$baseUrl/alert-codes/$alertCode",
    )
}

data class AlertCodeCreatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,

) : AlertCodeEvent() {
  override fun toDomainEvent(baseUrl: String) = toDomainEvent(DomainEventType.ALERT_CODE_CREATED, baseUrl)
}

data class AlertCodeDeactivatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertCodeEvent() {
  override fun toDomainEvent(baseUrl: String) = toDomainEvent(DomainEventType.ALERT_CODE_DEACTIVATED, baseUrl)
}

data class AlertCodeReactivatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertCodeEvent() {
  override fun toDomainEvent(baseUrl: String) = toDomainEvent(DomainEventType.ALERT_CODE_REACTIVATED, baseUrl)
}

data class AlertCodeUpdatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertCodeEvent() {
  override fun toDomainEvent(baseUrl: String) = toDomainEvent(DomainEventType.ALERT_CODE_UPDATED, baseUrl)
}

data class AlertsMergedEvent(
  val prisonNumberMergeFrom: String,
  val prisonNumberMergeTo: String,
  val mergedAlerts: List<MergedAlert>,
  val occurredAt: LocalDateTime = LocalDateTime.now(),
) {
  fun toDomainEvent(baseUrl: String): AlertDomainEvent<MergeAlertsAdditionalInformation> =
    AlertDomainEvent(
      eventType = ALERTS_MERGED.eventType,
      additionalInformation = MergeAlertsAdditionalInformation(
        url = "$baseUrl/prisoners/$prisonNumberMergeTo/alerts?size=2147483647",
        prisonNumberMergeFrom = prisonNumberMergeFrom,
        prisonNumberMergeTo = prisonNumberMergeTo,
        mergedAlerts = mergedAlerts,
        source = NOMIS,
      ),
      description = ALERTS_MERGED.description,
      occurredAt = occurredAt.toZoneDateTime(),
      detailUrl = "$baseUrl/prisoners/$prisonNumberMergeTo/alerts?size=2147483647",
      personReference = PersonReference.withPrisonNumber(prisonNumberMergeTo),
    )
}

abstract class AlertTypeEvent {
  abstract val alertCode: String
  abstract val occurredAt: LocalDateTime

  abstract fun toDomainEvent(baseUrl: String): AlertDomainEvent<ReferenceDataAdditionalInformation>

  protected fun toDomainEvent(
    type: DomainEventType,
    baseUrl: String,
  ): AlertDomainEvent<ReferenceDataAdditionalInformation> =
    AlertDomainEvent(
      eventType = type.eventType,
      additionalInformation = ReferenceDataAdditionalInformation(
        url = "$baseUrl/alert-types/$alertCode",
        alertCode = alertCode,
        source = DPS,
      ),
      description = type.description,
      occurredAt = occurredAt.toZoneDateTime(),
      detailUrl = "$baseUrl/alert-types/$alertCode",
    )
}

data class AlertTypeCreatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertTypeEvent() {
  override fun toDomainEvent(baseUrl: String) = toDomainEvent(DomainEventType.ALERT_TYPE_CREATED, baseUrl)
}

data class AlertTypeDeactivatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertTypeEvent() {
  override fun toDomainEvent(baseUrl: String) = toDomainEvent(DomainEventType.ALERT_TYPE_DEACTIVATED, baseUrl)
}

data class AlertTypeReactivatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertTypeEvent() {
  override fun toDomainEvent(baseUrl: String) = toDomainEvent(DomainEventType.ALERT_TYPE_REACTIVATED, baseUrl)
}

data class AlertTypeUpdatedEvent(
  override val alertCode: String,
  override val occurredAt: LocalDateTime,
) : AlertTypeEvent() {
  override fun toDomainEvent(baseUrl: String) = toDomainEvent(DomainEventType.ALERT_TYPE_UPDATED, baseUrl)
}
