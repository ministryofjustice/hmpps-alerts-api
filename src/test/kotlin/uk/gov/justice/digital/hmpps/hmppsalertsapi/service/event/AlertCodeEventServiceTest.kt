package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCodeCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.ReferenceDataAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CODE_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import java.time.LocalDateTime

class AlertCodeEventServiceTest {
  private val telemetryClient = mock<TelemetryClient>()
  private val domainEventPublisher = mock<DomainEventPublisher>()

  private val baseUrl = "http://localhost:8080"

  @Test
  fun `handle alert event - publish enabled`() {
    val eventProperties = EventProperties(true, baseUrl)
    val alertCodeEventService = AlertCodeEventService(eventProperties, telemetryClient, domainEventPublisher)
    val alertEvent = AlertCodeCreatedEvent(
      ALERT_CODE_VICTIM,
      LocalDateTime.now(),
    )

    alertCodeEventService.handleAlertEvent(alertEvent)

    val domainEventCaptor = argumentCaptor<AlertDomainEvent>()
    verify(domainEventPublisher).publish(domainEventCaptor.capture())
    assertThat(domainEventCaptor.firstValue).isEqualTo(
      AlertDomainEvent(
        eventType = ALERT_CODE_CREATED.eventType,
        additionalInformation = ReferenceDataAdditionalInformation(
          url = "$baseUrl/alert-codes/${ALERT_CODE_VICTIM}",
          alertCode = alertEvent.alertCode,
          source = DPS,
        ),
        description = ALERT_CODE_CREATED.description,
        occurredAt = alertEvent.occurredAt,
      ),
    )
  }

  @Test
  fun `handle alert event - publish disabled`() {
    val eventProperties = EventProperties(false, baseUrl)
    val alertCodeEventService = AlertCodeEventService(eventProperties, telemetryClient, domainEventPublisher)
    val alertEvent = AlertCodeCreatedEvent(
      ALERT_CODE_VICTIM,
      LocalDateTime.now(),
    )

    alertCodeEventService.handleAlertEvent(alertEvent)

    verify(domainEventPublisher, never()).publish(any<AlertDomainEvent>())
  }
}
