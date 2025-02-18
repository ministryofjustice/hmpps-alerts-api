package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan.Companion.BULK_ALERT_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.BulkPlan.Companion.BULK_ALERT_USERNAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Status
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.getPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.CREATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.AuditEventAction.UPDATED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode.KEEP_ALL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class StartBulkPlanIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/bulk-alerts/plan/${UUID.randomUUID()}/start")
      .exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [ROLE_PRISONER_ALERTS__RO, ROLE_PRISONER_ALERTS__RW])
  fun `403 forbidden - alerts reader`(role: String?) {
    startPlanResponseSpec(UUID.randomUUID(), role = role).expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = ["EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED", "KEEP_ALL"])
  fun `202 accepted - can start a plan`(cleanupModeString: String) {
    val cleanupMode = BulkAlertCleanupMode.valueOf(cleanupModeString)
    val (plan, people) = transactionTemplate.execute {
      val existingPeople = (0..16).map { givenPersonSummary(personSummary()) }
      val alertCode = givenAlertCode("XSA")
      existingPeople.forEachIndexed { index, personSummary ->
        if (index % 3 == 0) {
          givenAlert(
            alert(
              personSummary.prisonNumber,
              alertCode,
              activeTo = when {
                index % 2 == 0 -> LocalDate.now().plusDays(7)
                index % 5 == 0 -> LocalDate.now()
                else -> null
              },
              deletedAt = if (index % 4 == 0) {
                LocalDateTime.now().minusDays(1)
              } else {
                null
              },
            ),
          )
        }
      }
      (0..5).forEachIndexed { i, _ ->
        givenAlert(
          alert(
            prisonNumber(),
            alertCode,
            activeTo = if (i % 2 == 0) LocalDate.now().plusDays(10) else null,
            deletedAt = if (i % 5 == 0) LocalDateTime.now().minusDays(1) else null,
          ),
        )
      }

      val plan = givenBulkPlan(
        plan(alertCode, "A description for the bulk alert", cleanupMode)
          .apply { people.addAll(existingPeople) },
      )
      plan to existingPeople
    }!!

    val prisonNumbers = people.map { it.prisonNumber }.toSet()
    val existingAlertsToExpire = alertRepository.findAllActiveByCode("XSA")
      .filter { it.prisonNumber !in prisonNumbers }

    val startTime = LocalDateTime.now()
    startPlanResponseSpec(plan.id).expectStatus().isAccepted

    await withPollDelay ofSeconds(1) untilCallTo {
      bulkPlanRepository.getPlan(plan.id).completedAt
    } matches { it != null }

    val saved = bulkPlanRepository.getPlan(plan.id)
    assertThat(saved.startedAt!!).isCloseTo(startTime, within(5, ChronoUnit.SECONDS))
    assertThat(saved.startedBy).isEqualTo(TEST_USER)
    assertThat(saved.startedByDisplayName).isEqualTo(TEST_USER_NAME)
    assertThat(saved.createdCount).isEqualTo(14)
    assertThat(saved.updatedCount).isEqualTo(1)
    assertThat(saved.unchangedCount).isEqualTo(2)
    when (cleanupMode) {
      EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED -> assertThat(saved.expiredCount).isEqualTo(existingAlertsToExpire.size)
      KEEP_ALL -> assertThat(saved.expiredCount).isEqualTo(0)
    }

    val alerts = alertRepository.findAllActiveByCode("XSA").filter { it.prisonNumber in prisonNumbers }
    assertThat(alerts.size).isEqualTo(17)
    assertThat(alerts.map { it.activeTo }.all { it == null }).isTrue()

    val results = alerts.map {
      val auditEvent = it.auditEvents().first()
      when {
        auditEvent.actionedBy == BULK_ALERT_USERNAME && auditEvent.action == CREATED -> Status.CREATE to it
        auditEvent.actionedBy == BULK_ALERT_USERNAME && auditEvent.action == UPDATED -> Status.UPDATE to it
        else -> Status.ACTIVE to it
      }
    }.groupBy({ it.first }, { it.second })

    assertThat(results[Status.CREATE]).hasSize(14)
    assertThat(results[Status.UPDATE]).hasSize(1)
    assertThat(results[Status.ACTIVE]).hasSize(2)

    if (cleanupMode == EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED) {
      val expired = existingAlertsToExpire.map { alertRepository.findByIdOrNull(it.id)!! }
      assertThat(expired).hasSize(existingAlertsToExpire.size)
      expired.forEach {
        assertThat(it.activeTo).isEqualTo(LocalDate.now())
        with(it.auditEvents().first()) {
          assertThat(action).isEqualTo(AuditEventAction.INACTIVE)
          assertThat(actionedBy).isEqualTo(BULK_ALERT_USERNAME)
          assertThat(actionedByDisplayName).isEqualTo(BULK_ALERT_DISPLAY_NAME)
        }
      }
    }

    await withPollDelay ofSeconds(5) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches {
      it == (30 + (2 * saved.expiredCount!!)) + if (cleanupMode == EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED) 4 else 0
    }
    val messages = hmppsEventsQueue.receiveAllMessages()
    val created = messages.filter { it.eventType == DomainEventType.ALERT_CREATED.eventType }
    assertThat(created).hasSize(saved.createdCount!!)
    val updated = messages.filter { it.eventType == DomainEventType.ALERT_UPDATED.eventType }
    assertThat(updated).hasSize(saved.updatedCount!! + saved.expiredCount!!)
    assertThat((created + updated).mapNotNull { it.personReference.findNomsNumber() } - prisonNumbers).hasSize(saved.expiredCount!!)
    if (cleanupMode == EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED) {
      val expired = messages.filter { it.eventType == DomainEventType.ALERT_INACTIVE.eventType }
      assertThat(expired).hasSize(saved.expiredCount!!)
    }
    val personAlertsChanged = messages.filter { it.eventType == DomainEventType.PERSON_ALERTS_CHANGED.eventType }
    assertThat(personAlertsChanged).hasSize(15 + saved.expiredCount!!)
  }

  @Test
  fun `202 accepted - can handle multiple active alerts`() {
    val (plan, people) = transactionTemplate.execute {
      val existingPeople = (0..10).map { givenPersonSummary(personSummary()) }
      val alertCode = givenAlertCode("XNR")
      existingPeople.forEachIndexed { index, personSummary ->
        repeat(2) {
          if (index % 2 == 0) {
            givenAlert(
              alert(
                personSummary.prisonNumber,
                alertCode,
                activeTo = when {
                  index % 3 == 0 -> LocalDate.now().plusDays(7)
                  index % 5 == 0 -> LocalDate.now()
                  else -> null
                },
                deletedAt = if (index % 4 == 0) {
                  LocalDateTime.now().minusDays(1)
                } else {
                  null
                },
              ),
            )
          }
        }
      }
      (0..5).forEachIndexed { i, _ ->
        repeat(2) {
          givenAlert(
            alert(
              prisonNumber(),
              alertCode,
              activeTo = if (i % 2 == 0) LocalDate.now().plusDays(10) else null,
              deletedAt = if (i % 5 == 0) LocalDateTime.now().minusDays(1) else null,
            ),
          )
        }
      }

      val plan = givenBulkPlan(
        plan(alertCode, "A description for multiple active alert", EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED)
          .apply { people.addAll(existingPeople) },
      )
      plan to existingPeople
    }!!

    val prisonNumbers = people.map { it.prisonNumber }.toSet()
    val existingAlertsToExpire = alertRepository.findAllActiveByCode("XNR")
      .filter { it.prisonNumber !in prisonNumbers }

    val startTime = LocalDateTime.now()
    startPlanResponseSpec(plan.id).expectStatus().isAccepted

    await withPollDelay ofSeconds(1) untilCallTo {
      bulkPlanRepository.getPlan(plan.id).completedAt
    } matches { it != null }

    val saved = bulkPlanRepository.getPlan(plan.id)
    assertThat(saved.startedAt!!).isCloseTo(startTime, within(5, ChronoUnit.SECONDS))
    assertThat(saved.startedBy).isEqualTo(TEST_USER)
    assertThat(saved.startedByDisplayName).isEqualTo(TEST_USER_NAME)
    assertThat(saved.createdCount).isEqualTo(9)
    assertThat(saved.updatedCount).isEqualTo(2)
    assertThat(saved.unchangedCount).isEqualTo(2)
    assertThat(saved.expiredCount).isEqualTo(existingAlertsToExpire.size)

    val alerts = alertRepository.findAllActiveByCode("XNR").filter { it.prisonNumber in prisonNumbers }
    assertThat(alerts.size).isEqualTo(13)
    assertThat(alerts.map { it.activeTo }.all { it == null }).isTrue()

    val results = alerts.map {
      val auditEvent = it.auditEvents().first()
      when {
        auditEvent.actionedBy == BULK_ALERT_USERNAME && auditEvent.action == CREATED -> Status.CREATE to it
        auditEvent.actionedBy == BULK_ALERT_USERNAME && auditEvent.action == UPDATED -> Status.UPDATE to it
        else -> Status.ACTIVE to it
      }
    }.groupBy({ it.first }, { it.second })

    assertThat(results[Status.CREATE]).hasSize(saved.createdCount!!)
    assertThat(results[Status.UPDATE]).hasSize(saved.updatedCount!!)
    assertThat(results[Status.ACTIVE]).hasSize(saved.unchangedCount!!)
    val expired = existingAlertsToExpire.map { alertRepository.findByIdOrNull(it.id)!! }
    assertThat(expired).hasSize(existingAlertsToExpire.size)
    expired.forEach {
      assertThat(it.activeTo).isEqualTo(LocalDate.now())
      with(it.auditEvents().first()) {
        assertThat(action).isEqualTo(AuditEventAction.INACTIVE)
        assertThat(actionedBy).isEqualTo(BULK_ALERT_USERNAME)
        assertThat(actionedByDisplayName).isEqualTo(BULK_ALERT_DISPLAY_NAME)
      }
    }

    await withPollDelay ofSeconds(5) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches {
      it == 45
    }
    val messages = hmppsEventsQueue.receiveAllMessages()
    val created = messages.filter { it.eventType == DomainEventType.ALERT_CREATED.eventType }
    assertThat(created).hasSize(saved.createdCount!!)
    val updated = messages.filter { it.eventType == DomainEventType.ALERT_UPDATED.eventType }
    assertThat(updated).hasSize(saved.updatedCount!! + saved.expiredCount!!)
    assertThat((created + updated).mapNotNull { it.personReference.findNomsNumber() } - prisonNumbers).hasSize(saved.expiredCount!!)
    val expiredMessages = messages.filter { it.eventType == DomainEventType.ALERT_INACTIVE.eventType }
    assertThat(expiredMessages).hasSize(saved.expiredCount!!)
    val personAlertsChanged = messages.filter { it.eventType == DomainEventType.PERSON_ALERTS_CHANGED.eventType }
    assertThat(personAlertsChanged).hasSize(18)
  }

  private fun startPlanResponseSpec(
    id: UUID,
    username: String = TEST_USER,
    role: String? = ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI,
  ) = webTestClient.post().uri(URL, id)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setAlertRequestContext(username = username))
    .exchange()

  companion object {
    private const val URL = "/bulk-alerts/plan/{id}/start"
  }
}
