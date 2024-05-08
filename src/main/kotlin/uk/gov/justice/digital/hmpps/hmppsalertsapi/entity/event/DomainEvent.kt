package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Reason.USER
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
      "from source '${additionalInformation.source}' " +
      "with reason '${additionalInformation.reason}'"
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AlertAdditionalInformation::class, name = "alert"),
  JsonSubTypes.Type(value = ReferenceDataAdditionalInformation::class, name = "alertCode"),
)
abstract class AdditionalInformation {
  abstract val url: String
  abstract val source: Source
  abstract val reason: Reason
  abstract fun asString(): String
  abstract fun identifier(): String
}

data class AlertDomainEvent(
  override val eventType: String,
  override val additionalInformation: AdditionalInformation,
  override val version: Int = 1,
  override val description: String,
  override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent<AdditionalInformation>() {
  override fun toString(): String {
    return "v$version alert domain event '$eventType' " + additionalInformation.asString()
  }
}

data class AlertAdditionalInformation(
  override val url: String,
  val alertUuid: UUID,
  val prisonNumber: String,
  val alertCode: String,
  override val source: Source,
  override val reason: Reason,
) : AdditionalInformation() {
  override fun asString(): String =
    "for alert with UUID '$alertUuid' " +
      "for prison number '$prisonNumber' " +
      "with alert code '$alertCode' " +
      "from source '$source' " +
      "with reason '$reason'"

  override fun identifier(): String = alertUuid.toString()
}

data class ReferenceDataAdditionalInformation(
  override val url: String,
  val alertCode: String,
  override val source: Source,
  override val reason: Reason = USER,
) : AdditionalInformation() {
  override fun identifier(): String = alertCode

  override fun asString(): String =
    "for alert code '$alertCode' " +
      "from source '$source' " +
      "with reason '$reason'"
}
