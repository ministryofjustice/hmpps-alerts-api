package uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.ManageUsersExtension
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.wiremock.PrisonerSearchExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckTest : IntegrationTestBase() {

  @Test
  fun `Health page reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
      .jsonPath("components.db.status").isEqualTo("UP")
      .jsonPath("components.hmppsAuth.status").isEqualTo("UP")
      .jsonPath("components.manageUsers.status").isEqualTo("UP")
      .jsonPath("components.prisonerSearch.status").isEqualTo("UP")
  }

  @Test
  fun `Health page reports down`() {
    stubPingWithResponse(404)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .jsonPath("components.db.status").isEqualTo("UP")
      .jsonPath("components.hmppsAuth.status").isEqualTo("DOWN")
      .jsonPath("components.manageUsers.status").isEqualTo("DOWN")
      .jsonPath("components.prisonerSearch.status").isEqualTo("DOWN")
  }

  @Test
  fun `Health info reports version`() {
    stubPingWithResponse(200)

    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
        },
      )
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  private fun stubPingWithResponse(status: Int) {
    HmppsAuthApiExtension.hmppsAuth.stubHealthPing(status)
    ManageUsersExtension.manageUsers.stubHealthPing(status)
    PrisonerSearchExtension.prisonerSearch.stubHealthPing(status)
  }
}
