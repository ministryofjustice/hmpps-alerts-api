package uk.gov.justice.digital.hmpps.hmppsalertsapi.validator

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsalertsapi.utils.migrateAlert
import java.time.LocalDate

class MigrateAlertDateValidatorTest {
  private val underTest: MigrateAlertDateValidator = MigrateAlertDateValidator()

  @Test
  fun `same dates returns true`() {
    val result = underTest.isValid(migrateAlert().copy(activeFrom = LocalDate.now(), activeTo = LocalDate.now()), null)
    Assertions.assertThat(result).isTrue()
  }

  @Test
  fun `activeFrom before returns true`() {
    val result = underTest.isValid(migrateAlert().copy(activeFrom = LocalDate.now().minusDays(2), activeTo = LocalDate.now()), null)
    Assertions.assertThat(result).isTrue()
  }

  @Test
  fun `activeFrom after returns false`() {
    val result = underTest.isValid(migrateAlert().copy(activeFrom = LocalDate.now().plusDays(2), activeTo = LocalDate.now()), null)
    Assertions.assertThat(result).isFalse()
  }
}
