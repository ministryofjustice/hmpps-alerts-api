package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.BulkPlan

class CreateBulkPlanIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/bulk-alerts/plan")
      .exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/bulk-alerts/plan")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange().expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_PRISONER_ALERTS__RO, ROLE_PRISONER_ALERTS__RW])
  fun `403 forbidden - alerts reader`(role: String) {
    webTestClient.post()
      .uri("/bulk-alerts/plan")
      .headers(setAuthorisation(roles = listOf(role)))
      .headers(setAlertRequestContext())
      .exchange().expectStatus().isForbidden
  }

  @Test
  fun `201 created - new plan created and id returned`() {
    val plan = webTestClient.post()
      .uri("/bulk-alerts/plan")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
      .headers(setAlertRequestContext())
      .exchange().expectStatus().isCreated
      .expectBody<BulkPlan>().returnResult().responseBody!!

    assertThat(plan.id).isNotNull
  }
}
