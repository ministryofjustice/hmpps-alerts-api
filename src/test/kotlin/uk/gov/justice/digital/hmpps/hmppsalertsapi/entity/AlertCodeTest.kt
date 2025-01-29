package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AlertCodeTest {
  @Test
  fun `is active is true when deactivated at is null`() {
    val alertCode = alertCode().apply { deactivatedAt = null }
    assertThat(alertCode.isActive()).isTrue
  }

  @Test
  fun `is active is true when deactivated at is in the future`() {
    val alertCode = alertCode().apply { deactivatedAt = LocalDateTime.now().plusSeconds(3) }
    assertThat(alertCode.isActive()).isTrue
  }

  @Test
  fun `is active is true when deactivated at is in the past`() {
    val alertCode = alertCode().apply { deactivatedAt = LocalDateTime.now().minusSeconds(1) }
    assertThat(alertCode.isActive()).isFalse
  }

  private fun alertCode() = AlertCode(
    AlertType(
      "A",
      "Alert type A",
      1,
      LocalDateTime.now().minusDays(3),
      "CREATED_BY",
    ),
    "ABC",
    "Alert code ABC",
    1,
    LocalDateTime.now().minusDays(3),
    "CREATED_BY",
  ).apply {
    modifiedAt = LocalDateTime.now().minusDays(2)
    modifiedBy = "MODIFIED_BY"
  }
}
