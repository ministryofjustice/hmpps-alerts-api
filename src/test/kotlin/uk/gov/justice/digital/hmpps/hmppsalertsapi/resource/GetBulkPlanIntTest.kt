package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode.EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanAffect
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlanPrisoners
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.time.LocalDate
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
  fun `200 ok - can retrieve details from plan`() {
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
        plan(alertCode, "", EXPIRE_FOR_PRISON_NUMBERS_NOT_SPECIFIED)
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
    assertThat(affects.counts.toBeCreated).isEqualTo(11)
    assertThat(affects.counts.toBeUpdated).isEqualTo(4)
    assertThat(affects.counts.toBeExpired).isGreaterThanOrEqualTo(6)
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

  private inline fun <reified T> getPlan(path: String, id: UUID): T =
    getPlanResponseSpec(path, id).expectStatus().isOk
      .expectBody(T::class.java).returnResult().responseBody!!

  companion object {
    private const val BASE_URL = "/bulk-alerts/plan/{id}"
    private val PATHS = listOf("prisoners", "affects")

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
