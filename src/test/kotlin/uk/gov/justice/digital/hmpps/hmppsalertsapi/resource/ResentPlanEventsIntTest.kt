package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan.Companion.BULK_ALERT_USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.getPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.service.PlanBulkAlert
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.time.Duration.ofSeconds
import java.time.LocalDate

class ResentPlanEventsIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var planBulkAlert: PlanBulkAlert

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(BASE_URL, newUuid()).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post().uri(BASE_URL, newUuid())
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange().expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_PRISONER_ALERTS__RO, ROLE_PRISONER_ALERTS__RW])
  fun `403 forbidden - alerts reader or writer`(role: String) {
    webTestClient.post().uri(BASE_URL, newUuid())
      .headers(setAuthorisation(roles = listOf(role)))
      .headers(setAlertRequestContext())
      .exchange().expectStatus().isForbidden
  }

  @Test
  fun `202 accepted - plan events to be resent`() {
    val plan = transactionTemplate.execute {
      val existingPeople = (0..7).map { givenPersonSummary(personSummary()) }
      val alertCode = givenAlertCode("RLO")
      existingPeople.forEachIndexed { index, personSummary ->
        if (index % 2 == 0) {
          givenAlert(
            alert(
              personSummary.prisonNumber,
              alertCode,
              activeTo = when {
                index % 4 == 0 -> LocalDate.now()
                else -> null
              },
            ),
          )
        }
      }
      (0..3).forEachIndexed { i, _ ->
        givenAlert(
          alert(
            prisonNumber(),
            alertCode,
            activeTo = if (i % 2 == 0) LocalDate.now().plusDays(10) else null,
          ),
        )
      }

      givenBulkPlan(
        plan(alertCode, "A description for the bulk alert", EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED)
          .apply { people.addAll(existingPeople) },
      )
    }!!

    planBulkAlert.start(id = plan.id, context = AlertRequestContext(TEST_USER, TEST_USER_NAME))

    await withPollDelay ofSeconds(1) untilCallTo {
      bulkPlanRepository.getPlan(plan.id).completedAt
    } matches { it != null }

    `clear queues`()

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == null || it == 0 }

    webTestClient.post().uri(BASE_URL, plan.id)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange().expectStatus().isAccepted

    val saved = bulkPlanRepository.getPlan(plan.id)
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 24 }

    val auditEvents = auditEventRepository.findByActionedByAndActionedAt(BULK_ALERT_USERNAME, saved.startedAt!!)
    assertThat(auditEvents.size).isEqualTo(14)

    val messages = hmppsEventsQueue.receiveAllMessages()
    val created = messages.filter { it.eventType == DomainEventType.ALERT_CREATED.eventType }
    assertThat(created).hasSize(saved.createdCount!!)
    val updated = messages.filter { it.eventType == DomainEventType.ALERT_UPDATED.eventType }
    assertThat(updated).hasSize(saved.updatedCount!! + saved.expiredCount!!)
    val expired = messages.filter { it.eventType == DomainEventType.ALERT_INACTIVE.eventType }
    assertThat(expired).hasSize(saved.expiredCount!!)
    val personAlertsChanged = messages.filter { it.eventType == DomainEventType.PERSON_ALERTS_CHANGED.eventType }
    assertThat(personAlertsChanged).hasSize(10)
  }

  companion object {
    private const val BASE_URL = "/bulk-alerts/plan/{id}/events"
  }
}
