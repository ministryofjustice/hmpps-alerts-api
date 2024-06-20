package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
  fun `alert created event to string`() {
    val alertEvent = AlertCreatedEvent(UUID.randomUUID(), PRISON_NUMBER, ALERT_CODE_VICTIM, LocalDateTime.now(), NOMIS, TEST_USER)
    assertThat(alertEvent.toString()).isEqualTo(
      "Alert with UUID '${alertEvent.alertUuid}' " +
        "created for prison number '${alertEvent.prisonNumber}' " +
        "with alert code '${alertEvent.alertCode}' " +
        "at '${alertEvent.occurredAt}' " +
        "by '${alertEvent.createdBy}' " +
        "from source '${alertEvent.source}' ",
    )
  }

  @Test
  fun `alert created event to domain event`() {
    val alertEvent = AlertCreatedEvent(UUID.randomUUID(), PRISON_NUMBER, ALERT_CODE_VICTIM, LocalDateTime.now(), NOMIS, TEST_USER)

    val domainEvent = alertEvent.toDomainEvent(baseUrl)

    assertThat(domainEvent).isEqualTo(
      AlertDomainEvent(
        eventType = ALERT_CREATED.eventType,
        additionalInformation = AlertAdditionalInformation(
          url = "$baseUrl/alerts/${alertEvent.alertUuid}",
          alertUuid = alertEvent.alertUuid,
          prisonNumber = alertEvent.prisonNumber,
          alertCode = alertEvent.alertCode,
          source = alertEvent.source,
        ),
        description = ALERT_CREATED.description,
        occurredAt = alertEvent.occurredAt.toOffsetString(),
      ),
    )
    assertThat(domainEvent.toString()).isEqualTo(
      "v1 alert domain event '${ALERT_CREATED.eventType}' " +
        "for alert with UUID '${alertEvent.alertUuid}' " +
        "for prison number '${alertEvent.prisonNumber}' " +
        "with alert code '${alertEvent.alertCode}' " +
        "from source '${alertEvent.source}' ",
    )
  }

  @Test
  fun `alert updated event to string`() {
    val alertEvent = AlertUpdatedEvent(
      alertUuid = UUID.randomUUID(),
      prisonNumber = PRISON_NUMBER,
      alertCode = ALERT_CODE_VICTIM,
      occurredAt = LocalDateTime.now(),
      source = NOMIS,
      updatedBy = TEST_USER,
      descriptionUpdated = true,
      authorisedByUpdated = false,
      activeFromUpdated = true,
      activeToUpdated = false,
      commentAppended = true,
    )
    assertThat(alertEvent.toString()).isEqualTo(
      "Alert with UUID '${alertEvent.alertUuid}' " +
        "updated for prison number '${alertEvent.prisonNumber}' " +
        "with alert code '${alertEvent.alertCode}' " +
        "at '${alertEvent.occurredAt}' " +
        "by '${alertEvent.updatedBy}' " +
        "from source '${alertEvent.source}' " +
        "Properties updated: " +
        "description: true, " +
        "authorisedBy: false, " +
        "activeFrom: true, " +
        "activeTo: false, " +
        "comment appended: true.",
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
          url = "$baseUrl/alerts/${alertEvent.alertUuid}",
          alertUuid = alertEvent.alertUuid,
          prisonNumber = alertEvent.prisonNumber,
          alertCode = alertEvent.alertCode,
          source = alertEvent.source,
        ),
        description = ALERT_UPDATED.description,
        occurredAt = alertEvent.occurredAt.toOffsetString(),
      ),
    )
    assertThat(domainEvent.toString()).isEqualTo(
      "v1 alert domain event '${ALERT_UPDATED.eventType}' " +
        "for alert with UUID '${alertEvent.alertUuid}' " +
        "for prison number '${alertEvent.prisonNumber}' " +
        "with alert code '${alertEvent.alertCode}' " +
        "from source '${alertEvent.source}' ",
    )
  }

  @Test
  fun `alert deleted event to string`() {
    val alertEvent = AlertDeletedEvent(UUID.randomUUID(), PRISON_NUMBER, ALERT_CODE_VICTIM, LocalDateTime.now(), NOMIS, TEST_USER)
    assertThat(alertEvent.toString()).isEqualTo(
      "Alert with UUID '${alertEvent.alertUuid}' " +
        "deleted for prison number '${alertEvent.prisonNumber}' " +
        "with alert code '${alertEvent.alertCode}' " +
        "at '${alertEvent.occurredAt}' " +
        "by '${alertEvent.deletedBy}' " +
        "from source '${alertEvent.source}' ",
    )
  }

  @Test
  fun `alert deleted event to domain event`() {
    val alertEvent = AlertDeletedEvent(UUID.randomUUID(), PRISON_NUMBER, ALERT_CODE_VICTIM, LocalDateTime.now(), NOMIS, TEST_USER)

    val domainEvent = alertEvent.toDomainEvent(baseUrl)

    assertThat(domainEvent).isEqualTo(
      AlertDomainEvent(
        eventType = ALERT_DELETED.eventType,
        additionalInformation = AlertAdditionalInformation(
          url = "$baseUrl/alerts/${alertEvent.alertUuid}",
          alertUuid = alertEvent.alertUuid,
          prisonNumber = alertEvent.prisonNumber,
          alertCode = alertEvent.alertCode,
          source = alertEvent.source,
        ),
        description = ALERT_DELETED.description,
        occurredAt = alertEvent.occurredAt.toOffsetString(),
      ),
    )
    assertThat(domainEvent.toString()).isEqualTo(
      "v1 alert domain event '${ALERT_DELETED.eventType}' " +
        "for alert with UUID '${alertEvent.alertUuid}' " +
        "for prison number '${alertEvent.prisonNumber}' " +
        "with alert code '${alertEvent.alertCode}' " +
        "from source '${alertEvent.source}' ",
    )
  }
}
