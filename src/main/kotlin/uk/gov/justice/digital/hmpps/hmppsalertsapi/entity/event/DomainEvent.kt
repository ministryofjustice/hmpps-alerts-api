package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.MergedAlert
import java.time.ZonedDateTime
import java.util.UUID

interface DomainEvent {
  val eventType: String
  val version: Int
  val detailUrl: String?
  val occurredAt: ZonedDateTime
  val description: String?
  val additionalInformation: AdditionalInformation
  val personReference: PersonReference?
}

data class HmppsDomainEvent(
  override val eventType: String,
  override val version: Int = 1,
  override val detailUrl: String? = null,
  override val occurredAt: ZonedDateTime = ZonedDateTime.now(),
  override val description: String? = null,
  override val additionalInformation: HmppsAdditionalInformation = HmppsAdditionalInformation(),
  override val personReference: PersonReference = PersonReference(),
) : DomainEvent

data class PersonReference(val identifiers: List<PersonIdentifier> = listOf()) {
  operator fun get(key: String) = identifiers.find { it.type == key }?.value
  fun findNomsNumber() = get("NOMS")

  companion object {
    fun withPrisonNumber(prisonNumber: String) = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber)))
  }
}

data class PersonIdentifier(val type: String, val value: String)

abstract class AlertBaseDomainEvent<T : AlertBaseAdditionalInformation> : DomainEvent {
  abstract override val eventType: String
  abstract override val additionalInformation: T
  abstract override val version: Int
  abstract override val description: String
  abstract override val occurredAt: ZonedDateTime
  override val detailUrl: String? = null
  override val personReference: PersonReference? = null

  override fun toString(): String {
    return "v$version domain event '$eventType' " +
      "for resource '${additionalInformation.url}' " +
      "from source '${additionalInformation.source}' "
  }
}

interface AdditionalInformation

data class HmppsAdditionalInformation(private val mutableMap: MutableMap<String, Any?> = mutableMapOf()) :
  AdditionalInformation

interface AlertBaseAdditionalInformation : AdditionalInformation {
  val url: String
  val source: Source
  fun asString(): String
  fun identifier(): String
}

data class AlertDomainEvent<T : AlertBaseAdditionalInformation>(
  override val eventType: String,
  override val additionalInformation: T,
  override val version: Int = 1,
  override val description: String,
  override val occurredAt: ZonedDateTime,
  override val detailUrl: String? = null,
  override val personReference: PersonReference? = null,
) : AlertBaseDomainEvent<T>() {
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
) : AlertBaseAdditionalInformation {
  override fun asString(): String =
    "for alert with UUID '$alertUuid' " +
      "for prison number '$prisonNumber' " +
      "with alert code '$alertCode' " +
      "from source '$source' "

  override fun identifier(): String = alertUuid.toString()
}

data class MergeAlertsAdditionalInformation(
  override val url: String,
  val prisonNumberMergeFrom: String,
  val prisonNumberMergeTo: String,
  val mergedAlerts: List<MergedAlert>,
  override val source: Source,
) : AlertBaseAdditionalInformation {
  override fun asString(): String =
    "for prison number merged to '$prisonNumberMergeTo' " +
      "and prison number merged from'$prisonNumberMergeFrom' " +
      "with ${mergedAlerts.count()} merged alerts " +
      "from source '$source' "

  override fun identifier(): String = "$prisonNumberMergeFrom->$prisonNumberMergeTo"
}

data class ReferenceDataAdditionalInformation(
  override val url: String,
  val alertCode: String,
  override val source: Source,
) : AlertBaseAdditionalInformation {
  override fun identifier(): String = alertCode

  override fun asString(): String =
    "for alert code '$alertCode' " +
      "from source '$source' "
}
