package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonAlertsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.PersonReference
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter
import kotlin.time.measureTime

@Service
class PersonAlertsChangedEventService(
  private val eventProperties: EventProperties,
  private val domainEventPublisher: DomainEventPublisher,
  private val telemetryClient: TelemetryClient,
) {
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleEvent(event: PersonAlertsChangedEvent) {
    telemetryClient.trackEvent(
      "PublishingAlertsChanged",
      mapOf(
        "timestamp" to now().format(DateTimeFormatter.ISO_DATE_TIME),
      ),
      mapOf(),
    )
    val duration = measureTime {
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
    telemetryClient.trackEvent(
      "PublishedAlertsChanged",
      mapOf(
        "duration" to duration.inWholeMilliseconds.toString(),
        "timestamp" to now().format(DateTimeFormatter.ISO_DATE_TIME),
      ),
      mapOf(),
    )
  }
}
