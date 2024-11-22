package uk.gov.justice.digital.hmpps.hmppsalertsapi.service.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertCodeCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.AlertDomainEvent
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event.ReferenceDataAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CODE_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import java.time.LocalDateTime

class AlertCodeEventServiceTest {
  private val domainEventPublisher = mock<DomainEventPublisher>()

  private val baseUrl = "http://localhost:8080"

  @Test
  fun `handle alert event - publish enabled`() {
    val eventProperties = EventProperties(baseUrl)
    val alertCodeEventService = AlertReferenceDataEventService(eventProperties, domainEventPublisher)
    val alertEvent = AlertCodeCreatedEvent(
      ALERT_CODE_VICTIM,
      LocalDateTime.now(),
    )

    alertCodeEventService.handleAlertEvent(alertEvent)

    val domainEventCaptor = argumentCaptor<AlertDomainEvent<ReferenceDataAdditionalInformation>>()
    verify(domainEventPublisher).publishSingle(domainEventCaptor.capture())
    assertThat(domainEventCaptor.firstValue).isEqualTo(
      AlertDomainEvent(
        eventType = ALERT_CODE_CREATED.eventType,
        additionalInformation = ReferenceDataAdditionalInformation(
          alertCode = alertEvent.alertCode,
          source = DPS,
        ),
        description = ALERT_CODE_CREATED.description,
        occurredAt = alertEvent.occurredAt.toZoneDateTime(),
        detailUrl = "$baseUrl/alert-codes/${ALERT_CODE_VICTIM}",
      ),
    )
  }
}
