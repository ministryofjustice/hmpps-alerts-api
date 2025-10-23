package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertType
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertType
import java.util.Optional

class GetAlertTypesIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get()
      .uri("/alert-types")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get()
      .uri("/alert-types")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get alert types excludes inactive types and codes by default`() {
    val alertTypes = webTestClient.getAlertTypes(null)
    assertThat(alertTypes.all { it.isActive }).isTrue()
    assertThat(alertTypes.flatMap { it.alertCodes }.all { it.isActive }).isTrue()
  }

  @Test
  fun `get alert types including inactive types and codes`() {
    val alertTypes = webTestClient.getAlertTypes(true)
    assertThat(alertTypes.any { !it.isActive }).isTrue()
    assertThat(alertTypes.flatMap { it.alertCodes }.any { !it.isActive }).isTrue()
  }

  @Test
  fun `get specific alert type`() {
    val alertType = webTestClient.getAlertType("L")
    assertThat(alertType.code).isEqualTo("L")
    assertThat(alertType.alertCodes).hasSize(4)
  }

  @Test
  fun `get specific alert type - does not produce duplicates for restricted alerts`() {
    val alertCode = givenExistingAlertCode("LPQAA")
    givenNewAlertCodePrivilegedUser(alertCode = alertCode, username = "USER_1")
    givenNewAlertCodePrivilegedUser(alertCode = alertCode, username = "USER_2")
    givenNewAlertCodePrivilegedUser(alertCode = alertCode, username = "USER_3")

    val alertType = webTestClient.getAlertType("L")
    assertThat(alertType.code).isEqualTo("L")
    assertThat(alertType.alertCodes).hasSize(4)
  }

  private fun WebTestClient.getAlertTypes(includeInactive: Boolean?) = get()
    .uri { builder ->
      builder
        .path("/alert-types")
        .queryParamIfPresent("includeInactive", Optional.ofNullable(includeInactive))
        .build()
    }
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
    .headers(setAlertRequestContext())
    .exchange().expectStatus().isOk.expectBodyList<AlertType>().returnResult().responseBody!!

  private fun WebTestClient.getAlertType(alertTypeCode: String) = get()
    .uri { builder -> builder.path("/alert-types/$alertTypeCode").build() }
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO)))
    .headers(setAlertRequestContext())
    .exchange().successResponse<AlertType>()
}
