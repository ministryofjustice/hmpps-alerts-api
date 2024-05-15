package uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InfoTest : IntegrationTestBase() {

  @Test
  fun `Info page is accessible`() {
    webTestClient.get()
      .uri("/info")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("build.name").isEqualTo("hmpps-alerts-api")
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("build.version").value<String> {
        assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
      }
  }

  @Test
  fun `has active prisons`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("activeAgencies").value<List<String>> {
        assertThat(it.contains("***")).isTrue
      }
  }

  @Test
  fun `has navigation`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("navigation").value<HashMap<String, Object>> {
        assertThat(it["navEnabled"]).isEqualTo(false)
        assertThat(it["description"]).isEqualTo("Create and manage alert types and codes. Add alerts in bulk for lists of prisoners.")
        assertThat(it["href"]).isEqualTo("https://alerts-ui-dev.hmpps.service.justice.gov.uk")
      }
  }
}
