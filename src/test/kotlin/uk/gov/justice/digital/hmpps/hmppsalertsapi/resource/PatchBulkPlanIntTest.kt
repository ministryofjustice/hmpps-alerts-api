package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlan
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.BulkAction
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetAlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.SetDescription
import java.util.UUID

class PatchBulkPlanIntTest : IntegrationTestBase() {

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

  private fun patchPlan(id: UUID, action: Set<BulkAction>): BulkPlan {
    return webTestClient.patch()
      .uri("/bulk-alerts/plan/{id}", id)
      .bodyValue(action)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange().expectStatus().isOk
      .expectBody<BulkPlan>().returnResult().responseBody!!
  }
}
