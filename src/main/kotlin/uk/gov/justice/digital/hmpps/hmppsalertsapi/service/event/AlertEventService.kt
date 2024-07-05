package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertEvent

@Service
class AlertEventService(
  private val eventProperties: EventProperties,
  private val telemetryClient: TelemetryClient,
  private val domainEventPublisher: DomainEventPublisher,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleAlertEvent(event: AlertEvent) {
    if (eventProperties.publish) {
      val de = event.toDomainEvent(eventProperties.baseUrl)
      domainEventPublisher.publish(de)
      telemetryClient.trackEvent(de.eventType, mapOf("prisonNumber" to event.prisonNumber), null)
    } else {
      log.trace("{} publishing is disabled", event::class.simpleName)
    }
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
