package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertReferenceDataEvent

@Service
class AlertReferenceDataEventService(
  private val eventProperties: EventProperties,
  private val domainEventPublisher: DomainEventPublisher,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleAlertEvent(event: AlertReferenceDataEvent) {
    domainEventPublisher.publishSingle(event.toDomainEvent(eventProperties.baseUrl))
  }
}
