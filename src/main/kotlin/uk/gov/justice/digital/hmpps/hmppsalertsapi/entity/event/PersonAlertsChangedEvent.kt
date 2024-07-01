package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType

data class PersonAlertsChangedEvent(val prisonNumber: String) {
  val type = DomainEventType.PERSON_ALERTS_CHANGED
}
