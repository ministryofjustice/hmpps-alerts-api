package uk.gov.justice.digital.hmpps.hmppsalertsapi.validator

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.model.request.CreateAlert
import java.time.LocalDate

class ActiveDateRangeValidatorTest {
  private val underTest: ActiveDateRangeValidator = ActiveDateRangeValidator()

  @Test
  fun `same dates returns true`() {
    val result = underTest.isValid(CreateAlert("", "", "", LocalDate.now(), LocalDate.now()), null)
    Assertions.assertThat(result).isTrue()
  }

  @Test
  fun `activeFrom before returns true`() {
    val result = underTest.isValid(CreateAlert("", "", "", LocalDate.now().minusDays(2), LocalDate.now()), null)
    Assertions.assertThat(result).isTrue()
  }

  @Test
  fun `activeFrom after returns false`() {
    val result = underTest.isValid(CreateAlert("", "", "", LocalDate.now().plusDays(2), LocalDate.now()), null)
    Assertions.assertThat(result).isFalse()
  }
}
