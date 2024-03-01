package uk.gov.justice.digital.hmpps.hmppsalertsapi.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LocalDateExtTest {

  @Nested
  @DisplayName("onOrBefore")
  inner class OnOrBefore {
    @Test
    fun `returns true if date is on`() {
      val date = LocalDate.of(2022, 1, 1)
      assertThat(date.onOrBefore(date)).isTrue()
    }

    @Test
    fun `returns true if date is before`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayAfter = date.plusDays(1)
      assertThat(date.onOrBefore(dayAfter)).isTrue()
    }

    @Test
    fun `returns false if date is after`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayBefore = date.minusDays(1)
      assertThat(date.onOrBefore(dayBefore)).isFalse()
    }
  }

  @Nested
  @DisplayName("onOrAfter")
  inner class OnOrAfter {
    @Test
    fun `returns true if date is on`() {
      val date = LocalDate.of(2022, 1, 1)
      assertThat(date.onOrAfter(date)).isTrue()
    }

    @Test
    fun `returns true if date is after`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayBefore = date.minusDays(1)
      assertThat(date.onOrAfter(dayBefore)).isTrue()
    }

    @Test
    fun `returns false if date is after`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayAfter = date.plusDays(1)
      assertThat(date.onOrAfter(dayAfter)).isFalse()
    }
  }
}
