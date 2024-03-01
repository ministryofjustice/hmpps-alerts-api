package uk.gov.justice.digital.hmpps.hmppsalertsapi.validator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.UpdateAlert
import java.time.LocalDate

class UpdateAlertDateValidatorTest {
  private val underTest: UpdateAlertDateValidator = UpdateAlertDateValidator()

  @Test
  fun `same dates returns true`() {
    val result = underTest.isValid(UpdateAlert("", "", LocalDate.now(), LocalDate.now(), ""), null)
    assertThat(result).isTrue()
  }

  @Test
  fun `activeFrom before returns true`() {
    val result = underTest.isValid(UpdateAlert("", "", LocalDate.now().minusDays(2), LocalDate.now(), ""), null)
    assertThat(result).isTrue()
  }

  @Test
  fun `activeFrom after returns false`() {
    val result = underTest.isValid(UpdateAlert("", "", LocalDate.now().plusDays(2), LocalDate.now(), ""), null)
    assertThat(result).isFalse()
  }
}
