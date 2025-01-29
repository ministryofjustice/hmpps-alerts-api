package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
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

data class PersonReference(val identifiers: List<Identifier> = listOf()) {
  operator fun get(key: String) = identifiers.find { it.type == key }?.value
  fun findNomsNumber() = get(NOMS_NUMBER_TYPE)

  companion object {
    const val NOMS_NUMBER_TYPE = "NOMS"
    fun withPrisonNumber(prisonNumber: String) = PersonReference(listOf(Identifier(NOMS_NUMBER_TYPE, prisonNumber)))
  }

  data class Identifier(val type: String, val value: String)
}

abstract class AlertBaseDomainEvent<T : AlertBaseAdditionalInformation> : DomainEvent {
  abstract override val eventType: String
  abstract override val additionalInformation: T
  abstract override val version: Int
  abstract override val description: String
  abstract override val occurredAt: ZonedDateTime
  override val detailUrl: String? = null
  override val personReference: PersonReference? = null
}

interface AdditionalInformation

data class HmppsAdditionalInformation(private val mutableMap: MutableMap<String, Any?> = mutableMapOf()) :
  AdditionalInformation,
  MutableMap<String, Any?> by mutableMap

interface AlertBaseAdditionalInformation : AdditionalInformation {
  val source: Source
}

data class AlertDomainEvent<T : AlertBaseAdditionalInformation>(
  override val eventType: String,
  override val additionalInformation: T,
  override val version: Int = 1,
  override val description: String,
  override val occurredAt: ZonedDateTime,
  override val detailUrl: String? = null,
  override val personReference: PersonReference? = null,
) : AlertBaseDomainEvent<T>()

data class AlertAdditionalInformation(
  val alertUuid: UUID,
  val alertCode: String,
  override val source: Source,
) : AlertBaseAdditionalInformation

data class ReferenceDataAdditionalInformation(
  val alertCode: String,
  override val source: Source,
) : AlertBaseAdditionalInformation

val HmppsAdditionalInformation.nomsNumber get() = get("nomsNumber") as String
val HmppsAdditionalInformation.categoriesChanged get() = (get("categoriesChanged") as List<String>).toSet()
val HmppsAdditionalInformation.removedNomsNumber get() = get("removedNomsNumber") as String

object PersonChanged {
  val CATEGORIES_OF_INTEREST = setOf("PERSONAL_DETAILS", "STATUS", "LOCATION")
}
