package uk.gov.justice.digital.hmpps.hmppsalertsapi.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LanguageFormatUtilsTest {
  @Test
  fun `test formatDisplayName`() {
    val result = LanguageFormatUtils.formatDisplayName("JOHN SMITH-DOE")
    assertThat(result).isEqualTo("John Smith-Doe")
  }

  @Test
  fun `test formatDisplayName with underscore-seperated name`() {
    val result = LanguageFormatUtils.formatDisplayName("USER_NOT_FOUND")
    assertThat(result).isEqualTo("User Not Found")
  }
}
