package uk.gov.justice.digital.hmpps.hmppsalertsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.core.Ordered
import org.springframework.transaction.annotation.EnableTransactionManagement
import uk.gov.justice.digital.hmpps.hmppsalertsapi.config.EventProperties

@SpringBootApplication
@EnableConfigurationProperties(EventProperties::class)
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
class HmppsAlertsApi

fun main(args: Array<String>) {
  runApplication<HmppsAlertsApi>(*args)
}
