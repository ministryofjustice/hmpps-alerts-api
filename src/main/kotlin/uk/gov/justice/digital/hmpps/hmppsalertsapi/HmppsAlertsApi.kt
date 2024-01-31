package uk.gov.justice.digital.hmpps.hmppsalertsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsAlertsApi

fun main(args: Array<String>) {
  runApplication<HmppsAlertsApi>(*args)
}
