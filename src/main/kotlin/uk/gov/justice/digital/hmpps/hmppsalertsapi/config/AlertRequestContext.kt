package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.ALERTS_SERVICE
import java.time.LocalDateTime

data class AlertRequestContext(
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val source: Source = ALERTS_SERVICE,
  val username: String,
  val userDisplayName: String,
)
