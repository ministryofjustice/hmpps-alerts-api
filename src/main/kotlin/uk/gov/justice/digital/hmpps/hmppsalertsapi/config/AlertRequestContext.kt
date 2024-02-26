package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import java.time.LocalDateTime

data class AlertRequestContext(
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val username: String,
  val userDisplayName: String,
)
