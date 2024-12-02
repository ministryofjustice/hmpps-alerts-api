package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsalertsapi.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.BulkAlertCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.AddPrisonNumbers
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.RemovePrisonNumbers
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetAlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetCleanupMode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetDescription
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.IdGenerator.prisonNumber
import java.util.LinkedHashSet.newLinkedHashSet
import java.util.UUID

class PatchBulkPlanIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch().uri(URL, newUuid()).bodyValue(setOf<BulkAction>())
      .exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    patchPlanResponseSpec(newUuid(), setOf(SetAlertCode("CODE")), role = null).expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_PRISONER_ALERTS__RO, ROLE_PRISONER_ALERTS__RW])
  fun `403 forbidden - alerts reader`(role: String) {
    patchPlanResponseSpec(newUuid(), setOf(SetAlertCode("CODE")), role = role).expectStatus().isForbidden
  }

  @Test
  fun `404 not found - set alert code to non-existent value`() {
    val plan = givenBulkPlan(plan())
    val res = patchPlanResponseSpec(
      plan.id,
      setOf(SetAlertCode("NON_EXISTENT")),
    ).errorResponse(HttpStatus.NOT_FOUND)

    with(res) {
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("Not found: Alert code not found")
      assertThat(developerMessage).isEqualTo("Alert code not found with identifier NON_EXISTENT")
    }
  }

  @Test
  fun `400 bad request - invalid action params`() {
    val res = patchPlanResponseSpec(
      newUuid(),
      setOf(
        SetAlertCode("n".repeat(13)),
        SetDescription("n".repeat(256)),
        AddPrisonNumbers(newLinkedHashSet(0)),
      ),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(res) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo(
        """
        |Validation failures: 
        |Alert code must be supplied and be <= 12 characters
        |At least one prison number should be provided
        |Description must be <= 255 characters
        |
        """.trimMargin(),
      )
    }
  }

  @Test
  fun `400 bad request - invalid prison numbers`() {
    val plan = givenBulkPlan(plan())
    val res = patchPlanResponseSpec(
      plan.id,
      setOf(
        AddPrisonNumbers(
          newLinkedHashSet<String?>(5).apply {
            add(prisonNumber())
            add("Invalid")
            add(prisonNumber())
            add("ANOTHER_INVALID")
            add(prisonNumber())
          },
        ),
      ),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(res) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Prison number from rows 2, 4 were invalid")
    }
  }

  @Test
  fun `400 bad request - prison numbers not found`() {
    val plan = givenBulkPlan(plan())
    val validPns = setOf(prisonNumber(), prisonNumber())
    prisonerSearch.stubGetPrisoners(validPns)
    val res = patchPlanResponseSpec(
      plan.id,
      setOf(
        AddPrisonNumbers(
          newLinkedHashSet<String?>(5).apply {
            add(prisonNumber())
            addAll(validPns)
          },
        ),
      ),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(res) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Prison number from row 1 was invalid")
    }
  }

  @Test
  fun `200 ok - set the alert type and description of a new plan`() {
    val plan = givenBulkPlan(plan())
    val alertCode = givenAlertCode()
    val description = "A description about the alert"
    val res = patchPlan(
      plan.id,
      setOf(
        SetAlertCode(alertCode.code),
        SetDescription(description),
      ),
    )
    assertThat(res.id).isEqualTo(plan.id)

    val saved = requireNotNull(bulkPlanRepository.findByIdOrNull(plan.id))
    assertThat(saved.alertCode?.code).isEqualTo(alertCode.code)
    assertThat(saved.description).isEqualTo(description)
  }

  @Test
  fun `200 ok - set description to null for a plan`() {
    val plan = givenBulkPlan(plan())
    val res = patchPlan(
      plan.id,
      setOf(SetDescription(null)),
    )
    assertThat(res.id).isEqualTo(plan.id)

    val saved = requireNotNull(bulkPlanRepository.findByIdOrNull(plan.id))
    assertThat(saved.description).isNull()
  }

  @Test
  fun `200 ok - add prisoners to plan`() {
    val (plan, existingPeople) = transactionTemplate.execute {
      val existingPeople = listOf(givenPersonSummary(personSummary()), givenPersonSummary(personSummary()))
      val plan = givenBulkPlan(plan(givenAlertCode()).apply { people.addAll(existingPeople) })
      plan to existingPeople
    }!!
    val newPrisonNumbers = (1..10).map { _ -> prisonNumber() }
    val prisonNumbers: LinkedHashSet<String> = newLinkedHashSet<String>(12).apply {
      addAll(existingPeople.map { it.prisonNumber })
      addAll(newPrisonNumbers)
    }
    prisonerSearch.stubGetPrisoners(newPrisonNumbers)

    val res = patchPlan(
      plan.id,
      setOf(AddPrisonNumbers(prisonNumbers)),
    )
    assertThat(res.id).isEqualTo(plan.id)

    transactionTemplate.execute {
      val saved = requireNotNull(bulkPlanRepository.findByIdOrNull(plan.id))
      assertThat(saved.people.size).isEqualTo(12)
      assertThat(saved.people.map { it.prisonNumber }).containsExactlyInAnyOrderElementsOf(prisonNumbers)
    }
  }

  @Test
  fun `200 ok - remove prisoners from plan`() {
    val (plan, existingPeople) = transactionTemplate.execute {
      val existingPeople = (1..5).map {
        givenPersonSummary(personSummary())
      }
      val plan = givenBulkPlan(plan(givenAlertCode()).apply { people.addAll(existingPeople) })
      plan to existingPeople
    }!!
    assertThat(existingPeople.size).isEqualTo(5)
    val toRemove = listOf(existingPeople.first().prisonNumber, existingPeople.last().prisonNumber).toSet()

    val res = patchPlan(
      plan.id,
      setOf(
        RemovePrisonNumbers(
          newLinkedHashSet<String>(2).apply {
            addAll(toRemove)
          },
        ),
      ),
    )
    assertThat(res.id).isEqualTo(plan.id)

    transactionTemplate.execute {
      val saved = requireNotNull(bulkPlanRepository.findByIdOrNull(plan.id))
      assertThat(saved.people.map { it.prisonNumber })
        .containsExactlyInAnyOrderElementsOf(existingPeople.map { it.prisonNumber } - toRemove)
    }
  }

  @Test
  fun `200 ok - set the cleanup mode of a plan`() {
    val plan = givenBulkPlan(plan(alertCode = givenAlertCode()))
    assertThat(plan.cleanupMode).isNull()

    val res = patchPlan(
      plan.id,
      setOf(SetCleanupMode(BulkAlertCleanupMode.KEEP_ALL)),
    )
    assertThat(res.id).isEqualTo(plan.id)

    val saved = requireNotNull(bulkPlanRepository.findByIdOrNull(plan.id))
    assertThat(saved.cleanupMode).isEqualTo(BulkAlertCleanupMode.KEEP_ALL)
  }

  private fun patchPlanResponseSpec(
    id: UUID,
    actions: Set<BulkAction>,
    username: String = TEST_USER,
    role: String? = ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI,
  ) = webTestClient.patch().uri(URL, id)
    .bodyValue(actions)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setAlertRequestContext(username = username))
    .exchange()

  private fun patchPlan(id: UUID, actions: Set<BulkAction>): BulkPlan =
    patchPlanResponseSpec(id, actions).expectStatus().isOk
      .expectBody<BulkPlan>().returnResult().responseBody!!

  companion object {
    private const val URL = "/bulk-alerts/plan/{id}"
  }
}
