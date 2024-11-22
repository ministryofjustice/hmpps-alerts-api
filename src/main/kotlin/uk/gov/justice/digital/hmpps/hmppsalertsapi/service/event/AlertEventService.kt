package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.aop.DomainEventBatcher
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertEvent

@Service
class AlertEventService(
  private val eventProperties: EventProperties,
  private val domainEvents: DomainEventBatcher,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleAlertEvent(event: AlertEvent) {
    domainEvents.batchEvent(event.toDomainEvent(eventProperties.baseUrl))
  }
}
