package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import java.time.LocalDate

class PublishAlertInactiveEventsIntTest : IntegrationTestBase() {
  @Test
  fun `events emitted for alerts expiring today`() {
    val inactiveAlerts = (0..5).map { _ ->
      givenAlert(
        alert(
          givenPrisoner(),
          givenAlertCode(),
          activeFrom = LocalDate.now().minusDays(20),
          activeTo = LocalDate.now(),
        ),
      )
    }

    val activeAlerts = (0..5).map {
      givenAlert(
        alert(
          givenPrisoner(),
          givenAlertCode(),
          activeTo = if (it % 2 == 0) LocalDate.now().plusDays(7) else null,
        ),
      )
    }

    webTestClient.post().uri("/alerts/inactive").headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RW)))
      .exchange().expectStatus().is2xxSuccessful

    val msgs = hmppsEventsQueue.receiveAllMessages()
    val prisonNumbers = inactiveAlerts.map { it.prisonNumber }.toSet()
    val alertIds = inactiveAlerts.map { it.id.toString() }.toSet()
    assertThat(msgs.map { it.personReference.findNomsNumber() }.toSet()).containsAll(prisonNumbers)
    val msgAlertIds = msgs.mapNotNull { it.additionalInformation["alertUuid"] }.toSet()
    assertThat(msgAlertIds).containsAll(alertIds)
    assertThat(msgAlertIds).doesNotContainAnyElementsOf(activeAlerts.map { it.id.toString() }.toSet())
    prisonNumbers.forEach { pn ->
      assertThat(
        msgs.count {
          pn == it.personReference.findNomsNumber() && it.eventType == DomainEventType.PERSON_ALERTS_CHANGED.eventType
        },
      ).isEqualTo(1)
    }
    alertIds.forEach { id ->
      assertThat(
        msgs.count {
          it.eventType == DomainEventType.ALERT_INACTIVE.eventType && it.additionalInformation["alertUuid"] == id
        },
      ).isEqualTo(1)
    }
  }
}
