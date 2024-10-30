package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonAlertsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonReference

@Service
class PersonAlertsChangedEventService(
  private val eventProperties: EventProperties,
  private val domainEventPublisher: DomainEventPublisher,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleEvent(event: PersonAlertsChangedEvent) {
    domainEventPublisher.publish(
      HmppsDomainEvent(
        eventType = event.type.eventType,
        detailUrl = "${eventProperties.baseUrl}/prisoners/${event.prisonNumber}/alerts",
        additionalInformation = HmppsAdditionalInformation(),
        personReference = PersonReference.withPrisonNumber(event.prisonNumber),
        description = event.type.description,
      ),
    )
  }
}
