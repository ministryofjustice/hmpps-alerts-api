package uk.gov.justice.digital.hmpps.hmppsalertsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties

@SpringBootApplication
@EnableConfigurationProperties(EventProperties::class)
class HmppsAlertsApi

fun main(args: Array<String>) {
  runApplication<HmppsAlertsApi>(*args)
}
