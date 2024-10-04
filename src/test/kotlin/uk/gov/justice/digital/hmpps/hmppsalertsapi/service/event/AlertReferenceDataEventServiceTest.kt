package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertTypeCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.ReferenceDataAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_TYPE_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import java.time.LocalDateTime

class AlertReferenceDataEventServiceTest {
  private val telemetryClient = mock<TelemetryClient>()
  private val domainEventPublisher = mock<DomainEventPublisher>()

  private val baseUrl = "http://localhost:8080"

  @Test
  fun `handle alert event - publish enabled`() {
    val eventProperties = EventProperties(baseUrl)
    val alertReferenceDataEventService = AlertReferenceDataEventService(eventProperties, telemetryClient, domainEventPublisher)
    val alertEvent = AlertTypeCreatedEvent(
      ALERT_CODE_VICTIM,
      LocalDateTime.now(),
    )

    alertReferenceDataEventService.handleAlertEvent(alertEvent)

    val domainEventCaptor = argumentCaptor<AlertDomainEvent<ReferenceDataAdditionalInformation>>()
    verify(domainEventPublisher).publish(domainEventCaptor.capture())
    assertThat(domainEventCaptor.firstValue).isEqualTo(
      AlertDomainEvent(
        eventType = ALERT_TYPE_CREATED.eventType,
        additionalInformation = ReferenceDataAdditionalInformation(
          alertCode = alertEvent.alertCode,
          source = DPS,
        ),
        description = ALERT_TYPE_CREATED.description,
        occurredAt = alertEvent.occurredAt.toZoneDateTime(),
        detailUrl = "$baseUrl/alert-types/${ALERT_CODE_VICTIM}",
      ),
    )
  }
}
