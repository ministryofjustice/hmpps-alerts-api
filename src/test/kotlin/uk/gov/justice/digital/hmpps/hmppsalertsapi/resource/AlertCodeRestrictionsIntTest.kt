package uk.gov.justice.digital.hmpps.hmppsalertsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.AlertCodePrivilegedUserId
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.AlertCode
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.EntityGenerator.alertCode

class AlertCodeRestrictionsIntTest : IntegrationTestBase() {

  @Nested
  inner class RestrictAlertCode {
    @Test
    fun `401 unauthorised`() {
      webTestClient.patch()
        .uri("/alert-codes/VI/restrict")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `403 forbidden - no roles`() {
      webTestClient.patch()
        .uri("/alert-codes/VI/restrict")
        .headers(setAuthorisation())
        .headers(setAlertRequestContext())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `403 forbidden - alerts RW`() {
      webTestClient.patch()
        .uri("/alert-codes/VI/restrict")
        .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO, ROLE_PRISONER_ALERTS__RW)))
        .headers(setAlertRequestContext())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `404 alert code not found`() {
      val response = webTestClient.patch()
        .uri("/alert-codes/ALK/restrict")
        .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
        .headers(setAlertRequestContext())
        .exchange().errorResponse(NOT_FOUND)

      with(response) {
        assertThat(status).isEqualTo(404)
        assertThat(errorCode).isNull()
        assertThat(userMessage).isEqualTo("Not found: Alert code not found")
        assertThat(developerMessage).isEqualTo("Alert code not found with identifier ALK")
        assertThat(moreInfo).isNull()
      }
    }

    @Test
    fun `should update restricted status`() {
      val alertCode = givenNewAlertCode(alertCode(code = "ACRA", restricted = false))
      val response = webTestClient.restrictAlertCode(alertCode.code)
      assertThat(response.isRestricted).isTrue()
    }

    private fun WebTestClient.restrictAlertCode(alertCode: String): AlertCode = patch()
      .uri("/alert-codes/$alertCode/restrict")
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = true,
        ),
      )
      .exchange().successResponse()
  }

  @Nested
  inner class RemoveAlertCodeRestriction {
    @Test
    fun `401 unauthorised`() {
      webTestClient.patch()
        .uri("/alert-codes/VI/remove-restriction")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `403 forbidden - no roles`() {
      webTestClient.patch()
        .uri("/alert-codes/VI/remove-restriction")
        .headers(setAuthorisation())
        .headers(setAlertRequestContext())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `403 forbidden - alerts RW`() {
      webTestClient.patch()
        .uri("/alert-codes/VI/remove-restriction")
        .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO, ROLE_PRISONER_ALERTS__RW)))
        .headers(setAlertRequestContext())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `404 alert code not found`() {
      val response = webTestClient.patch()
        .uri("/alert-codes/ALK/remove-restriction")
        .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
        .headers(setAlertRequestContext())
        .exchange().errorResponse(NOT_FOUND)

      with(response) {
        assertThat(status).isEqualTo(404)
        assertThat(errorCode).isNull()
        assertThat(userMessage).isEqualTo("Not found: Alert code not found")
        assertThat(developerMessage).isEqualTo("Alert code not found with identifier ALK")
        assertThat(moreInfo).isNull()
      }
    }

    @Test
    fun `should remove restricted status`() {
      val alertCode = givenNewAlertCode(alertCode(code = "ACRB", restricted = true))
      val response = webTestClient.removeAlertCodeRestriction(alertCode.code)
      assertThat(response.isRestricted).isFalse()
    }

    private fun WebTestClient.removeAlertCodeRestriction(alertCode: String): AlertCode = patch()
      .uri("/alert-codes/$alertCode/remove-restriction")
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = true,
        ),
      )
      .exchange().successResponse()
  }

  @Nested
  inner class AddPrivilegedUser {
    @Test
    fun `401 unauthorised`() {
      webTestClient.post()
        .uri("/alert-codes/VI/privileged-user/USERNAME_1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `403 forbidden - no roles`() {
      webTestClient.post()
        .uri("/alert-codes/VI/privileged-user/USERNAME_1")
        .headers(setAuthorisation())
        .headers(setAlertRequestContext())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `403 forbidden - alerts RW`() {
      webTestClient.post()
        .uri("/alert-codes/VI/privileged-user/USERNAME_1")
        .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO, ROLE_PRISONER_ALERTS__RW)))
        .headers(setAlertRequestContext())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `404 alert code not found`() {
      val response = webTestClient.post()
        .uri("/alert-codes/ALK/privileged-user/USERNAME_1")
        .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
        .headers(setAlertRequestContext())
        .exchange().errorResponse(NOT_FOUND)

      with(response) {
        assertThat(status).isEqualTo(404)
        assertThat(errorCode).isNull()
        assertThat(userMessage).isEqualTo("Not found: Alert code not found")
        assertThat(developerMessage).isEqualTo("Alert code not found with identifier ALK")
        assertThat(moreInfo).isNull()
      }
    }

    @Test
    fun `should add privileged user`() {
      val alertCode = givenNewAlertCode(alertCode(code = "ACRC", restricted = true))
      val username = "USERNAME_1"

      webTestClient.addPrivilegedUser(alertCode.code, username)

      assertThat(
        alertCodePrivilegedUserRepository.findById(AlertCodePrivilegedUserId(alertCode.alertCodeId, username)),
      ).isNotEmpty
    }

    private fun WebTestClient.addPrivilegedUser(alertCode: String, username: String) = post()
      .uri("/alert-codes/$alertCode/privileged-user/$username")
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = true,
        ),
      )
      .exchange().expectStatus().isEqualTo(HttpStatus.OK)
  }

  @Nested
  inner class DeletePrivilegedUser {
    @Test
    fun `401 unauthorised`() {
      webTestClient.delete()
        .uri("/alert-codes/VI/privileged-user/USERNAME_1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `403 forbidden - no roles`() {
      webTestClient.delete()
        .uri("/alert-codes/VI/privileged-user/USERNAME_1")
        .headers(setAuthorisation())
        .headers(setAlertRequestContext())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `403 forbidden - alerts RW`() {
      webTestClient.delete()
        .uri("/alert-codes/VI/privileged-user/USERNAME_1")
        .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__RO, ROLE_PRISONER_ALERTS__RW)))
        .headers(setAlertRequestContext())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `404 alert code not found`() {
      val response = webTestClient.delete()
        .uri("/alert-codes/ALK/privileged-user/USERNAME_1")
        .headers(setAuthorisation(roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI)))
        .headers(setAlertRequestContext())
        .exchange().errorResponse(NOT_FOUND)

      with(response) {
        assertThat(status).isEqualTo(404)
        assertThat(errorCode).isNull()
        assertThat(userMessage).isEqualTo("Not found: Alert code not found")
        assertThat(developerMessage).isEqualTo("Alert code not found with identifier ALK")
        assertThat(moreInfo).isNull()
      }
    }

    @Test
    fun `should delete existing privileged user`() {
      val alertCode = givenNewAlertCode(alertCode(code = "ACRD", restricted = true))
      val username = "USERNAME_1"
      givenNewAlertCodePrivilegedUser(alertCode, username)

      webTestClient.deletePrivilegedUser(alertCode.code, username)

      assertThat(
        alertCodePrivilegedUserRepository.findById(AlertCodePrivilegedUserId(alertCode.alertCodeId, username)),
      ).isEmpty
    }

    @Test
    fun `should return success response when privileged user does not exist`() {
      val alertCode = givenNewAlertCode(alertCode(code = "ACRE", restricted = true))
      val username = "USERNAME_2"

      webTestClient.deletePrivilegedUser(alertCode.code, username)

      assertThat(
        alertCodePrivilegedUserRepository.findById(AlertCodePrivilegedUserId(alertCode.alertCodeId, username)),
      ).isEmpty
    }

    private fun WebTestClient.deletePrivilegedUser(alertCode: String, username: String) = delete()
      .uri("/alert-codes/$alertCode/privileged-user/$username")
      .headers(
        setAuthorisation(
          user = TEST_USER,
          roles = listOf(ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI),
          isUserToken = true,
        ),
      )
      .exchange().expectStatus().isEqualTo(HttpStatus.OK)
  }
}
