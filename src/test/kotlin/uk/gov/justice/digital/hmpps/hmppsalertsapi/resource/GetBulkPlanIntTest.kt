package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.AlertRequestContext
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode.KEEP_ALL
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanAffect
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanCounts
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanPrisoners
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanStatus
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class GetBulkPlanIntTest : IntegrationTestBase() {

  @ParameterizedTest
  @MethodSource("pathSource")
  fun `403 forbidden - alerts reader`(path: String) {
    getPlanResponseSpec(path, newUuid(), roles = listOf(ROLE_PRISONER_ALERTS__RO, ROLE_PRISONER_ALERTS__RW))
      .expectStatus().isForbidden
  }

  @ParameterizedTest
  @MethodSource("pathSource")
  fun `404 not found - set alert code to non-existent value`(path: String) {
    val fakeId = newUuid()
    val res = getPlanResponseSpec(path, fakeId).errorResponse(HttpStatus.NOT_FOUND)

    with(res) {
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("Not found: Plan not found")
      assertThat(developerMessage).isEqualTo("Plan not found with identifier $fakeId")
    }
  }

  @Test
  fun `400 bad request - cannot get affects if alert code not set`() {
    val plan = transactionTemplate.execute {
      val existingPeople = (1..5).map { givenPersonSummary(personSummary()) }
      val alertCode = givenAlertCode()
      givenAlert(alert(prisonNumber(), alertCode))
      givenBulkPlan(plan(null, null, KEEP_ALL).apply { people.addAll(existingPeople) })
    }!!

    val res = getPlanResponseSpec("affects", plan.id).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(400)
    assertThat(res.userMessage).isEqualTo("Validation failure: Unable to calculate affect of plan until the alert code is selected")
  }

  @Test
  fun `400 bad request - cannot get affects if cleanup mode not set`() {
    val plan = transactionTemplate.execute {
      val existingPeople = (1..5).map { givenPersonSummary(personSummary()) }
      val alertCode = givenAlertCode()
      givenAlert(alert(prisonNumber(), alertCode))
      givenBulkPlan(plan(alertCode, null, null).apply { people.addAll(existingPeople) })
    }!!

    val res = getPlanResponseSpec("affects", plan.id).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(400)
    assertThat(res.userMessage).isEqualTo("Validation failure: Unable to calculate affect of plan until the cleanup mode is selected")
  }

  @ParameterizedTest
  @ValueSource(strings = ["KEEP_ALL", "EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED"])
  fun `200 ok - can retrieve details from plan`(cleanupModeString: String) {
    val cleanupMode = BulkAlertCleanupMode.valueOf(cleanupModeString)
    val (plan, existingPeople) = transactionTemplate.execute {
      val existingPeople = (0..16).map { givenPersonSummary(personSummary()) }
      val alertCode = givenAlertCode()
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
            ),
          )
        }
      }
      (0..5).forEach { _ -> givenAlert(alert(prisonNumber(), alertCode)) }

      val plan = givenBulkPlan(
        plan(alertCode, "A description for the bulk alert", cleanupMode)
          .apply { people.addAll(existingPeople) },
      )
      plan to existingPeople
    }!!

    val prisoners = getPlan<BulkPlanPrisoners>("prisoners", plan.id)
    prisoners.prisoners.forEach {
      it.verifyAgainst(existingPeople.get(it.prisonNumber))
    }

    val affects = getPlan<BulkPlanAffect>("affects", plan.id)
    assertThat(affects.counts.existingAlerts).isEqualTo(2)
    assertThat(affects.counts.created).isEqualTo(12)
    assertThat(affects.counts.updated).isEqualTo(3)
    when (cleanupMode) {
      EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED -> assertThat(affects.counts.expired).isGreaterThanOrEqualTo(6)
      KEEP_ALL -> assertThat(affects.counts.expired).isEqualTo(0)
    }

    val saved = bulkPlanRepository.save(
      plan
        .start(AlertRequestContext("STARTED_BY", "Started By Display Name"))
        .completed(9, 3, 7, 12),
    )

    val status = getPlan<BulkPlanStatus>("status", plan.id)
    with(status) {
      assertThat(createdBy).isEqualTo(saved.createdBy)
      assertThat(createdByDisplayName).isEqualTo(saved.createdByDisplayName)
      assertThat(startedBy).isEqualTo(saved.startedBy)
      assertThat(startedByDisplayName).isEqualTo(saved.startedByDisplayName)
      assertThat(completedAt).isCloseTo(saved.completedAt, within(1, ChronoUnit.SECONDS))
      assertThat(counts).isEqualTo(BulkPlanCounts(7, 9, 3, 12))
    }
  }

  private fun List<PersonSummary>.get(prisonNumber: String): PersonSummary = first { it.prisonNumber == prisonNumber }

  private fun getPlanResponseSpec(
    path: String,
    id: UUID,
    username: String = TEST_USER,
    roles: List<String> = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
  ) = webTestClient.get().uri("$BASE_URL/$path", id)
    .headers(setAuthorisation(roles = roles))
    .headers(setAlertRequestContext(username = username))
    .exchange()

  private inline fun <reified T : Any> getPlan(path: String, id: UUID): T = getPlanResponseSpec(path, id).expectStatus().isOk
    .expectBody<T>().returnResult().responseBody!!

  companion object {
    private const val BASE_URL = "/bulk-alerts/plan/{id}"
    private val PATHS = listOf("prisoners", "affects", "status")

    @JvmStatic
    fun pathSource(): List<Arguments> = PATHS.map { Arguments.of(it) }
  }
}

private fun PrisonerSummary.verifyAgainst(person: PersonSummary) {
  assertThat(prisonNumber).isEqualTo(person.prisonNumber)
  assertThat(lastName).isEqualTo(person.lastName)
  assertThat(firstName).isEqualTo(person.firstName)
  assertThat(prisonCode).isEqualTo(person.prisonCode)
  assertThat(cellLocation).isEqualTo(person.cellLocation)
}
