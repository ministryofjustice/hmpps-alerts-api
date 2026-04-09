package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class ClockConfiguration(
  private val zoneId: ZoneId,
) {
  @Bean
  fun clock(): Clock = Clock.system(zoneId)
}
