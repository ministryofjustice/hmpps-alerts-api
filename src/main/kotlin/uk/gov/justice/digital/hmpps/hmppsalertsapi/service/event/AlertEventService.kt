package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCreatedEvent

@Service
class AlertEventService(
  private val eventProperties: EventProperties,
  private val telemetryClient: TelemetryClient,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleAlertEvent(event: AlertCreatedEvent) {
    log.info(
      "Alert with UUID '${event.alertUuid}' " +
        "created for prison number '${event.prisonNumber}' " +
        "with alert code '${event.alertCode}' " +
        "at '${event.occurredAt}' " +
        "by '${event.createdBy}' " +
        "from source '${event.source}'.",
    )
    log.info("Event properties: $eventProperties")
  }

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
