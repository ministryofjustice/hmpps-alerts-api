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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Reason.USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import java.time.LocalDateTime
import java.util.UUID

class AlertEventServiceTest {
  private val telemetryClient = mock<TelemetryClient>()
  private val domainEventPublisher = mock<DomainEventPublisher>()

  private val baseUrl = "http://localhost:8080"

  @Test
  fun `handle alert event - publish enabled`() {
    val eventProperties = EventProperties(true, baseUrl)
    val alertEventService = AlertEventService(eventProperties, telemetryClient, domainEventPublisher)
    val alertEvent = AlertCreatedEvent(
      UUID.randomUUID(),
      PRISON_NUMBER,
      ALERT_CODE_VICTIM,
      LocalDateTime.now(),
      NOMIS,
      USER,
      TEST_USER,
    )

    alertEventService.handleAlertEvent(alertEvent)

    val domainEventCaptor = argumentCaptor<AlertDomainEvent>()
    verify(domainEventPublisher).publish(domainEventCaptor.capture())
    assertThat(domainEventCaptor.firstValue).isEqualTo(
      AlertDomainEvent(
        eventType = ALERT_CREATED.eventType,
        additionalInformation = AlertAdditionalInformation(
          url = "$baseUrl/alerts/${alertEvent.alertUuid}",
          alertUuid = alertEvent.alertUuid,
          prisonNumber = alertEvent.prisonNumber,
          alertCode = alertEvent.alertCode,
          source = alertEvent.source,
          reason = alertEvent.reason,
        ),
        description = ALERT_CREATED.description,
        occurredAt = alertEvent.occurredAt,
      ),
    )
  }

  @Test
  fun `handle alert event - publish disabled`() {
    val eventProperties = EventProperties(false, baseUrl)
    val alertEventService = AlertEventService(eventProperties, telemetryClient, domainEventPublisher)
    val alertEvent = AlertCreatedEvent(
      UUID.randomUUID(),
      PRISON_NUMBER,
      ALERT_CODE_VICTIM,
      LocalDateTime.now(),
      NOMIS,
      USER,
      TEST_USER,
    )

    alertEventService.handleAlertEvent(alertEvent)

    verify(domainEventPublisher, never()).publish(any<AlertDomainEvent>())
  }
}
