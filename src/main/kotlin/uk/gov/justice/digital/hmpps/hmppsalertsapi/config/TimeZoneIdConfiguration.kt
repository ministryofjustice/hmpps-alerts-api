package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.ZoneId

@Configuration
class TimeZoneIdConfiguration(
  @param:Value($$"${spring.jackson.time-zone}")
  private val timeZone: String,
) {
  @Bean
  fun timeZoneId(): ZoneId = ZoneId.of(timeZone)
}
