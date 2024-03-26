package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppsalertsapi.enumeration.Source.DPS
import java.time.LocalDateTime

data class AlertRequestContext(
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val source: Source = DPS,
  val username: String,
  val userDisplayName: String,
  val activeCaseLoadId: String?,
)
