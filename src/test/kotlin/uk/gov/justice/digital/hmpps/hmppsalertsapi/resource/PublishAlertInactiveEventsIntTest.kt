package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.ALERT_INACTIVE
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType.PERSON_ALERTS_CHANGED
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

    webTestClient.post().uri("/alerts/inactive").exchange().expectStatus().is2xxSuccessful

    // call a second time - should only produce one audit event and one domain event
    webTestClient.post().uri("/alerts/inactive").exchange().expectStatus().is2xxSuccessful

    val msgs = hmppsEventsQueue.receiveAllMessages()
    val prisonNumbers = inactiveAlerts.map { it.prisonNumber }.toSet()
    val alertIds = inactiveAlerts.map { it.id }.toSet()
    assertThat(msgs.map { it.personReference.findNomsNumber() }.toSet()).containsAll(prisonNumbers)
    val msgAlertIds = msgs.mapNotNull { it.additionalInformation["alertUuid"] }.toSet()
    assertThat(msgAlertIds).containsAll(alertIds.map { it.toString() })
    assertThat(msgAlertIds).doesNotContainAnyElementsOf(activeAlerts.map { it.id.toString() }.toSet())
    prisonNumbers.forEach { pn ->
      assertThat(
        msgs.count {
          pn == it.personReference.findNomsNumber() && it.eventType == PERSON_ALERTS_CHANGED.eventType
        },
      ).isEqualTo(1)
    }
    alertIds.forEach { id ->
      assertThat(
        msgs.count {
          it.eventType == ALERT_INACTIVE.eventType && it.additionalInformation["alertUuid"] == id.toString()
        },
      ).isEqualTo(1)
      with(requireNotNull(alertRepository.findByIdOrNull(id))) {
        assertThat(auditEvents().count { it.action == AuditEventAction.INACTIVE }).isEqualTo(1)
      }
    }
  }
}
