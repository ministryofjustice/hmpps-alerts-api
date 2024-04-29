package uk.gov.justice.digital.hmpps.hmppsalertsapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AlertTypeTest {

  @Test
  fun `is active is true when deactivated at is null`() {
    val alertType = alertType().apply { deactivatedAt = null }
    assertThat(alertType.isActive()).isTrue
  }

  @Test
  fun `is active is true when deactivated at is in the future`() {
    val alertType = alertType().apply { deactivatedAt = LocalDateTime.now().plusSeconds(3) }
    assertThat(alertType.isActive()).isTrue
  }

  @Test
  fun `is active is true when deactivated at is in the past`() {
    val alertType = alertType().apply { deactivatedAt = LocalDateTime.now().minusSeconds(1) }
    assertThat(alertType.isActive()).isFalse
  }

  private fun alertType() =
    AlertType(
      1,
      "A",
      "Alert type A",
      1,
      LocalDateTime.now().minusDays(3),
      "CREATED_BY",
    ).apply {
      modifiedAt = LocalDateTime.now().minusDays(2)
      modifiedBy = "MODIFIED_BY"
    }

  private fun alertCode(alertType: AlertType, alertCodeId: Long = 1, code: String = "ABC") =
    AlertCode(
      alertCodeId,
      alertType,
      code,
      "Alert code $code",
      1,
      LocalDateTime.now().minusDays(3),
      "CREATED_BY",
    ).apply {
      modifiedAt = LocalDateTime.now().minusDays(2)
      modifiedBy = "MODIFIED_BY"
      alertType.addAlertCode(this)
    }
}
