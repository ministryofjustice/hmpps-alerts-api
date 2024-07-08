package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.common.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_DELETED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.ALERT_CODE_VICTIM
import java.time.LocalDateTime
import java.util.UUID

class AlertEventTest {
  private val baseUrl = "http://localhost:8080"

  @Test
  fun `alert created event to domain event`() {
    val alertEvent =
      AlertCreatedEvent(UUID.randomUUID(), PRISON_NUMBER, ALERT_CODE_VICTIM, LocalDateTime.now(), NOMIS, TEST_USER)

    val domainEvent = alertEvent.toDomainEvent(baseUrl)

    assertThat(domainEvent).isEqualTo(
      AlertDomainEvent(
        eventType = ALERT_CREATED.eventType,
        additionalInformation = AlertAdditionalInformation(
          alertUuid = alertEvent.alertUuid,
          alertCode = alertEvent.alertCode,
          source = alertEvent.source,
        ),
        description = ALERT_CREATED.description,
        occurredAt = alertEvent.occurredAt.toZoneDateTime(),
        detailUrl = "$baseUrl/alerts/${alertEvent.alertUuid}",
        personReference = PersonReference.withPrisonNumber(alertEvent.prisonNumber),
      ),
    )
  }

  @Test
  fun `alert updated event to domain event`() {
    val alertEvent = AlertUpdatedEvent(
      alertUuid = UUID.randomUUID(),
      prisonNumber = PRISON_NUMBER,
      alertCode = ALERT_CODE_VICTIM,
      occurredAt = LocalDateTime.now(),
      source = NOMIS,
      updatedBy = TEST_USER,
      descriptionUpdated = true,
      authorisedByUpdated = true,
      activeFromUpdated = true,
      activeToUpdated = true,
      commentAppended = true,
    )

    val domainEvent = alertEvent.toDomainEvent(baseUrl)

    assertThat(domainEvent).isEqualTo(
      AlertDomainEvent(
        eventType = ALERT_UPDATED.eventType,
        additionalInformation = AlertAdditionalInformation(
          alertUuid = alertEvent.alertUuid,
          alertCode = alertEvent.alertCode,
          source = alertEvent.source,
        ),
        description = ALERT_UPDATED.description,
        occurredAt = alertEvent.occurredAt.toZoneDateTime(),
        detailUrl = "$baseUrl/alerts/${alertEvent.alertUuid}",
        personReference = PersonReference.withPrisonNumber(alertEvent.prisonNumber),
      ),
    )
  }

  @Test
  fun `alert deleted event to domain event`() {
    val alertEvent =
      AlertDeletedEvent(UUID.randomUUID(), PRISON_NUMBER, ALERT_CODE_VICTIM, LocalDateTime.now(), NOMIS, TEST_USER)

    val domainEvent = alertEvent.toDomainEvent(baseUrl)

    assertThat(domainEvent).isEqualTo(
      AlertDomainEvent(
        eventType = ALERT_DELETED.eventType,
        additionalInformation = AlertAdditionalInformation(
          alertUuid = alertEvent.alertUuid,
          alertCode = alertEvent.alertCode,
          source = alertEvent.source,
        ),
        description = ALERT_DELETED.description,
        occurredAt = alertEvent.occurredAt.toZoneDateTime(),
        detailUrl = "$baseUrl/alerts/${alertEvent.alertUuid}",
        personReference = PersonReference.withPrisonNumber(alertEvent.prisonNumber),
      ),
    )
  }
}
