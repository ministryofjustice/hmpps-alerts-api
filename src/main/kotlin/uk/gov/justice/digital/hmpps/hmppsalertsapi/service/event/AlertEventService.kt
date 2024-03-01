package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertUpdatedEvent

@Service
class AlertEventService(
  private val eventProperties: EventProperties,
  // private val telemetryClient: TelemetryClient,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleAlertEvent(event: AlertCreatedEvent) {
    log.info(event.toString())

    // TODO: Publish create alert domain event

    // TODO: Track event for metrics
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleAlertEvent(event: AlertUpdatedEvent) {
    log.info(event.toString())

    // TODO: Publish create alert domain event

    // TODO: Track event for metrics
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleAlertEvent(event: AlertDeletedEvent) {
    log.info(event.toString())

    // TODO: Publish create alert domain event

    // TODO: Track event for metrics
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
