package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertCode
import java.util.Optional

class RetrieveAlertCodeIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.get()
      .uri("/alert-codes")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.get()
      .uri("/alert-codes")
      .headers(setAuthorisation())
      .headers(setAlertRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get alert codes excludes inactive codes by default`() {
    val alertCodes = webTestClient.getAlertCodes(null)
    assertThat(alertCodes.all { it.isActive }).isTrue()
  }

  @Test
  fun `get alert codes including inactive codes`() {
    val alertCodes = webTestClient.getAlertCodes(true)
    assertThat(alertCodes.any { !it.isActive }).isTrue()
  }

  @Test
  fun `get alert codes shows if each code can be administered by the user`() {
    val restrictedAlertCode = givenNewAlertCode(alertCode(code = "XRACA", restricted = true))
    val restrictedAlertCodeWithPermissionForUser = givenNewAlertCode(alertCode(code = "XRACB", restricted = true))
    val restrictedAlertCodeWithPermissionForOtherUser = givenNewAlertCode(alertCode(code = "XRACC", restricted = true))
    givenNewAlertCodePrivilegedUser(restrictedAlertCodeWithPermissionForUser)
    givenNewAlertCodePrivilegedUser(restrictedAlertCodeWithPermissionForOtherUser, "SOME_OTHER_USER")

    val alertCodes = webTestClient.getAlertCodes(true)

    alertCodes.first { it.code == restrictedAlertCode.code }.let {
      assertThat(it.isRestricted).isTrue()
      assertThat(it.canBeAdministered).isFalse()
    }
    alertCodes.first { it.code == restrictedAlertCodeWithPermissionForUser.code }.let {
      assertThat(it.isRestricted).isTrue()
      assertThat(it.canBeAdministered).isTrue()
    }
    alertCodes.first { it.code == restrictedAlertCodeWithPermissionForOtherUser.code }.let {
      assertThat(it.isRestricted).isTrue()
      assertThat(it.canBeAdministered).isFalse()
    }
  }

  @Test
  fun `get specific alert code`() {
    val alertCodes = webTestClient.getAlertCode("VI")
    assertThat(alertCodes.code).isEqualTo("VI")
  }

  private fun WebTestClient.getAlertCodes(includeInactive: Boolean?) = get()
    .uri { builder ->
      builder
        .path("/alert-codes")
        .queryParamIfPresent("includeInactive", Optional.ofNullable(includeInactive))
        .build()
    }
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
    .headers(setAlertRequestContext())
    .exchange().expectStatus().isOk.expectBodyList<AlertCode>().returnResult().responseBody!!

  private fun WebTestClient.getAlertCode(alertCode: String) = get()
    .uri { builder ->
      builder
        .path("/alert-codes/$alertCode")
        .build()
    }
    .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
    .headers(setAlertRequestContext())
    .exchange().successResponse<AlertCode>()
}
